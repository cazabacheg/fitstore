package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.event.ItemCantidad;
import com.fitstore.catalog.event.StockNoConfirmado;
import com.fitstore.catalog.inventario.dto.ItemReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import com.fitstore.catalog.inventario.model.EstadoReserva;
import com.fitstore.catalog.inventario.model.Reserva;
import com.fitstore.catalog.inventario.model.ReservaItem;
import com.fitstore.catalog.inventario.model.TipoMovimiento;
import com.fitstore.catalog.inventario.repository.ConfiguracionInventarioRepository;
import com.fitstore.catalog.inventario.repository.ReservaItemRepository;
import com.fitstore.catalog.inventario.repository.ReservaRepository;
import com.fitstore.catalog.inventario.repository.StockConsultas;
import com.fitstore.catalog.inventario.repository.StockConsultas.Saldo;
import com.fitstore.catalog.inventario.repository.StockRepository;
import com.fitstore.common.event.OutboxRegistrador;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservaServiceImpl implements ReservaService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservaServiceImpl.class);

    private final ReservaRepository reservas;
    private final ReservaItemRepository items;
    private final StockRepository stock;
    private final StockConsultas consultas;
    private final ConfiguracionInventarioRepository configuracion;
    private final AlertaStockBajo alerta;
    private final RegistroMovimientos movimientos;
    private final ObjectProvider<OutboxRegistrador> outbox;
    private final EntityManager em;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate propia;

    public ReservaServiceImpl(ReservaRepository reservas, ReservaItemRepository items, StockRepository stock,
            StockConsultas consultas, ConfiguracionInventarioRepository configuracion, AlertaStockBajo alerta,
            RegistroMovimientos movimientos, ObjectProvider<OutboxRegistrador> outbox, EntityManager em,
            PlatformTransactionManager gestor) {
        this.reservas = reservas;
        this.items = items;
        this.stock = stock;
        this.consultas = consultas;
        this.configuracion = configuracion;
        this.alerta = alerta;
        this.movimientos = movimientos;
        this.outbox = outbox;
        this.em = em;
        this.transaccion = new TransactionTemplate(gestor);
        this.propia = new TransactionTemplate(gestor);
    }

    // ---------------------------------------------------------------- T1: reservar

    @Override
    public Resultado reservar(ReservaRequest peticion) {
        Optional<ReservaDto> existente = leer(peticion.pedidoId());
        if (existente.isPresent()) {
            return new Resultado(existente.get(), false);
        }
        try {
            ReservaDto nueva = transaccion.execute(estado -> crear(peticion));
            return new Resultado(nueva, true);
        } catch (DataIntegrityViolationException | PersistenceException e) {
            // dos reintentos a la vez: el otro ganó el insert y esta transacción se deshizo; se devuelve la suya
            return new Resultado(leer(peticion.pedidoId()).orElseThrow(() -> e), false);
        }
    }

    private Optional<ReservaDto> leer(UUID pedidoId) {
        return reservas.findById(pedidoId).map(r -> aDto(r, items.findByPedidoId(pedidoId)));
    }

    /** Todas las líneas en una sola transacción, ordenadas por variante para no cruzar bloqueos (micro 4.6). */
    private ReservaDto crear(ReservaRequest peticion) {
        Map<Long, Integer> pedidas = new TreeMap<>();
        for (ItemReservaDto linea : peticion.items()) {
            pedidas.merge(linea.varianteId(), linea.cantidad(), Integer::sum);
        }
        List<Map<String, Object>> faltantes = new ArrayList<>();
        pedidas.forEach((varianteId, cantidad) -> {
            if (stock.reservarSiAlcanza(varianteId, cantidad) == 0) {
                int disponible = Math.max(0, consultas.disponible(varianteId).orElse(0));
                faltantes.add(Map.of("varianteId", varianteId, "solicitado", cantidad, "disponible", disponible));
            }
        });
        if (!faltantes.isEmpty()) {
            throw new StockInsuficienteException(faltantes);
        }

        int margen = configuracion.findById(1L).orElseThrow().getMargenReservaMinutos();
        Reserva reserva = new Reserva();
        reserva.setPedidoId(peticion.pedidoId());
        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setExpiraEn(peticion.expiraEnPedido().plus(margen, ChronoUnit.MINUTES));
        em.persist(reserva); // persist (no save): si el pedido ya tiene reserva, el INSERT debe fallar
        List<ReservaItem> lineas = new ArrayList<>();
        pedidas.forEach((varianteId, cantidad) -> {
            ReservaItem item = new ReservaItem();
            item.setPedidoId(peticion.pedidoId());
            item.setVarianteId(varianteId);
            item.setCantidad(cantidad);
            em.persist(item);
            lineas.add(item);
        });
        em.flush();
        pedidas.forEach((varianteId, cantidad) ->
                consultas.disponible(varianteId).ifPresent(d -> alerta.evaluar(varianteId, d, -cantidad)));
        return aDto(reserva, lineas);
    }

    private static ReservaDto aDto(Reserva r, List<ReservaItem> lineas) {
        List<ItemReservaDto> dtos = lineas.stream().sorted(Comparator.comparing(ReservaItem::getVarianteId))
                .map(i -> new ItemReservaDto(i.getVarianteId(), i.getCantidad())).toList();
        return new ReservaDto(r.getPedidoId(), r.getEstado().name(), r.getExpiraEn(), dtos);
    }

    // ---------------------------------------------------------------- T5: confirmar al pagar

    @Override
    @Transactional
    public void confirmarPago(UUID pedidoId, String codigoPedido, List<ItemCantidad> itemsDelEvento) {
        Optional<Reserva> encontrada = reservas.buscarConBloqueo(pedidoId);
        if (encontrada.isPresent()) {
            Reserva reserva = encontrada.get();
            switch (reserva.getEstado()) {
                case ACTIVA -> confirmarActiva(reserva, codigoPedido);
                case CONFIRMADA, REINGRESADA -> LOG.info("El pedido {} ya estaba pagado; se ignora", pedidoId);
                default -> tomarStockTardio(pedidoId, codigoPedido, itemsDelEvento, reserva);
            }
            return;
        }
        tomarStockTardio(pedidoId, codigoPedido, itemsDelEvento, null);
    }

    private void confirmarActiva(Reserva reserva, String codigoPedido) {
        for (ReservaItem item : items.findByPedidoId(reserva.getPedidoId())) {
            if (stock.descontar(item.getVarianteId(), item.getCantidad()) == 0) {
                LOG.warn("Stock inconsistente al confirmar la variante {} del pedido {}", item.getVarianteId(), reserva.getPedidoId());
                continue;
            }
            Saldo saldo = consultas.saldo(item.getVarianteId()).orElseThrow();
            movimientos.registrar(item.getVarianteId(), TipoMovimiento.VENTA, -item.getCantidad(), saldo.fisico(),
                    "Venta del pedido " + codigoPedido, codigoPedido, null);
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservas.saveAndFlush(reserva);
    }

    /**
     * El pago llegó con la reserva vencida, liberada o inexistente (decisión del lead, micro 4.7 y CA-2-52): se intenta tomar el
     * stock como en una reserva nueva, todo o nada. Si no alcanza no se descuenta nada, se registra la incidencia y se
     * publica {@code StockNoConfirmado} con los faltantes.
     */
    private void tomarStockTardio(UUID pedidoId, String codigoPedido, List<ItemCantidad> itemsDelEvento, Reserva previa) {
        Map<Long, Integer> pedidas = new TreeMap<>();
        itemsDelEvento.forEach(i -> pedidas.merge(i.varianteId(), i.cantidad(), Integer::sum));
        Map<Long, Integer> disponibles = consultas.bloquearDisponibles(pedidas.keySet());

        List<StockNoConfirmado.Faltante> faltantes = new ArrayList<>();
        pedidas.forEach((varianteId, cantidad) -> {
            int disponible = Math.max(0, disponibles.getOrDefault(varianteId, 0));
            if (disponible < cantidad) {
                String sku = consultas.datosAlerta(varianteId).map(StockConsultas.DatosAlerta::sku).orElse(null);
                faltantes.add(new StockNoConfirmado.Faltante(varianteId, sku, cantidad, disponible));
            }
        });
        if (!faltantes.isEmpty()) {
            LOG.warn("Pedido {} ({}) pagado sin stock suficiente: {}", pedidoId, codigoPedido, faltantes);
            OutboxRegistrador registrador = outbox.getIfAvailable();
            if (registrador != null) {
                registrador.registrar("StockNoConfirmado", pedidoId.toString(), new StockNoConfirmado(pedidoId, faltantes));
            }
            return;
        }
        pedidas.forEach((varianteId, cantidad) -> {
            Saldo saldo = consultas.aplicarDelta(varianteId, -cantidad, true).orElseThrow();
            movimientos.registrar(varianteId, TipoMovimiento.VENTA, -cantidad, saldo.fisico(),
                    "Venta del pedido " + codigoPedido, codigoPedido, null);
            alerta.evaluar(varianteId, saldo.disponible(), -cantidad);
        });
        if (previa != null) {
            previa.setEstado(EstadoReserva.CONFIRMADA);
            reservas.saveAndFlush(previa);
        } else {
            Reserva nueva = new Reserva();
            nueva.setPedidoId(pedidoId);
            nueva.setEstado(EstadoReserva.CONFIRMADA);
            nueva.setExpiraEn(Instant.now());
            em.persist(nueva);
            pedidas.forEach((varianteId, cantidad) -> {
                ReservaItem item = new ReservaItem();
                item.setPedidoId(pedidoId);
                item.setVarianteId(varianteId);
                item.setCantidad(cantidad);
                em.persist(item);
            });
            em.flush();
        }
    }

    // ---------------------------------------------------------------- compensaciones

    @Override
    @Transactional
    public void cancelar(UUID pedidoId, String codigoPedido) {
        reservas.buscarConBloqueo(pedidoId).ifPresent(reserva -> {
            switch (reserva.getEstado()) {
                case ACTIVA -> liberar(reserva, EstadoReserva.LIBERADA);
                case CONFIRMADA -> reingresar(reserva, codigoPedido);
                default -> LOG.info("La reserva del pedido {} ya estaba {}; se ignora", pedidoId, reserva.getEstado());
            }
        });
    }

    @Override
    @Transactional
    public void expirar(UUID pedidoId) {
        reservas.buscarConBloqueo(pedidoId).ifPresent(reserva -> {
            if (reserva.getEstado() == EstadoReserva.ACTIVA) {
                liberar(reserva, EstadoReserva.LIBERADA);
            } else {
                LOG.info("La reserva del pedido {} ya estaba {}; se ignora", pedidoId, reserva.getEstado());
            }
        });
    }

    private void liberar(Reserva reserva, EstadoReserva nuevoEstado) {
        for (ReservaItem item : items.findByPedidoId(reserva.getPedidoId())) {
            if (stock.liberar(item.getVarianteId(), item.getCantidad()) == 0) {
                LOG.warn("Stock inconsistente al liberar la variante {} del pedido {}", item.getVarianteId(), reserva.getPedidoId());
            }
        }
        reserva.setEstado(nuevoEstado);
        reservas.saveAndFlush(reserva);
    }

    /** El pedido ya estaba pagado: lo vendido vuelve al físico con un movimiento REINGRESO. */
    private void reingresar(Reserva reserva, String codigoPedido) {
        for (ReservaItem item : items.findByPedidoId(reserva.getPedidoId())) {
            Saldo saldo = consultas.aplicarDelta(item.getVarianteId(), item.getCantidad(), false).orElseThrow();
            movimientos.registrar(item.getVarianteId(), TipoMovimiento.REINGRESO, item.getCantidad(), saldo.fisico(),
                    "Cancelación del pedido " + codigoPedido, codigoPedido, null);
        }
        reserva.setEstado(EstadoReserva.REINGRESADA);
        reservas.saveAndFlush(reserva);
    }

    // ---------------------------------------------------------------- respaldo

    @Override
    public int liberarVencidas() {
        int liberadas = 0;
        for (Reserva candidata : reservas.findByEstadoAndExpiraEnBefore(EstadoReserva.ACTIVA, Instant.now())) {
            UUID pedidoId = candidata.getPedidoId();
            Boolean liberada = propia.execute(estado -> reservas.buscarConBloqueo(pedidoId)
                    .filter(r -> r.getEstado() == EstadoReserva.ACTIVA && r.getExpiraEn().isBefore(Instant.now()))
                    .map(r -> {
                        liberar(r, EstadoReserva.EXPIRADA);
                        return true;
                    }).orElse(false));
            if (Boolean.TRUE.equals(liberada)) {
                liberadas++;
                LOG.info("Reserva vencida del pedido {} liberada", pedidoId);
            }
        }
        return liberadas;
    }
}
