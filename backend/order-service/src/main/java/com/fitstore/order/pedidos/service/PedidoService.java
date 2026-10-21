package com.fitstore.order.pedidos.service;

import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.order.event.BoletaEmitida;
import com.fitstore.order.event.EntregaActualizada;
import com.fitstore.order.event.PagoConfirmado;
import com.fitstore.order.event.PagoNoAplicable;
import com.fitstore.order.event.PagoRechazado;
import com.fitstore.order.event.PedidoExpirado;
import com.fitstore.order.event.PedidoPagado;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.service.dominio.MaquinaEstadosPedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transiciones de estado del pedido (micro 4.4 a 4.6). Las que llegan por eventos (pago, expiración, entrega) usan una
 * actualización condicional {@code UPDATE … WHERE estado = desde}: si dos transiciones compiten, gana una y la otra
 * encuentra el estado ya cambiado.
 */
@Service
public class PedidoService {

    private static final Logger LOG = LoggerFactory.getLogger(PedidoService.class);
    private static final int MAX_COMENTARIO = 300;

    private final PedidoRepository pedidos;
    private final OutboxRegistrador outbox;
    private final MaquinaEstadosPedido maquina;
    private final TransactionTemplate transaccion;

    public PedidoService(PedidoRepository pedidos, OutboxRegistrador outbox, MaquinaEstadosPedido maquina,
            TransactionTemplate transaccion) {
        this.pedidos = pedidos;
        this.outbox = outbox;
        this.maquina = maquina;
        this.transaccion = transaccion;
    }

    // ---------- pago (T3) ----------

    /** {@code PagoConfirmado}: PENDIENTE_PAGO → PAGADO; si ya expiró o se canceló, avisa a payment para reembolsar. */
    @Transactional
    public void aplicarPagoConfirmado(PagoConfirmado evento) {
        Optional<Pedido> existente = pedidos.findById(evento.pedidoId());
        if (existente.isEmpty()) {
            LOG.warn("PagoConfirmado de un pedido desconocido: {}", evento.pedidoId());
            return;
        }
        Pedido pedido = existente.get();
        if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
            Optional<Pedido> pagado = transicionar(pedido.getId(), EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO);
            if (pagado.isPresent()) {
                Pedido p = pagado.get();
                p.setPagoProveedor(evento.proveedor());
                p.setPagoMedio(evento.medio());
                p.setPagadoEn(evento.confirmadoEn() == null ? Instant.now() : evento.confirmadoEn());
                p.agregarHistorial(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, TipoActor.SISTEMA, null,
                        "Pago confirmado");
                pedidos.save(p);
                outbox.registrar("PedidoPagado", p.getId().toString(),
                        new PedidoPagado(p.getId(), p.getCodigo(), PedidoMapper.cantidades(p)));
                return;
            }
            // Otra transición ganó la carrera: se decide con el estado que quedó
            pedido = pedidos.findById(pedido.getId()).orElseThrow();
        }
        if (pedido.getEstado() == EstadoPedido.EXPIRADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            String motivo = pedido.getEstado() == EstadoPedido.EXPIRADO ? "PEDIDO_EXPIRADO" : "PEDIDO_CANCELADO";
            outbox.registrar("PagoNoAplicable", pedido.getId().toString(),
                    new PagoNoAplicable(pedido.getId(), evento.intencionId(), motivo));
        }
    }

    /** {@code PagoRechazado}: el estado no cambia; se anota en el historial y el cliente puede reintentar. */
    @Transactional
    public void aplicarPagoRechazado(PagoRechazado evento) {
        pedidos.findById(evento.pedidoId())
                .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE_PAGO)
                .ifPresent(p -> {
                    p.agregarHistorial(p.getEstado(), p.getEstado(), TipoActor.SISTEMA, null,
                            recortar("Pago rechazado: " + evento.motivo()));
                    pedidos.save(p);
                });
    }

    /** {@code BoletaEmitida}: guarda el enlace de descarga. */
    @Transactional
    public void aplicarBoleta(BoletaEmitida evento) {
        pedidos.findById(evento.pedidoId()).ifPresent(p -> {
            p.setBoletaUrl(evento.url());
            pedidos.save(p);
        });
    }

    // ---------- expiración (T4) ----------

    /** Expira los PENDIENTE_PAGO vencidos; cada uno en su transacción. Devuelve cuántos expiró. */
    public int expirarVencidos() {
        List<UUID> vencidos = pedidos.findByEstadoAndExpiraEnBefore(EstadoPedido.PENDIENTE_PAGO, Instant.now())
                .stream().map(Pedido::getId).toList();
        int expirados = 0;
        for (UUID id : vencidos) {
            if (Boolean.TRUE.equals(transaccion.execute(estado -> expirar(id)))) {
                expirados++;
            }
        }
        return expirados;
    }

    /** PENDIENTE_PAGO → EXPIRADO si nadie ganó antes; devuelve {@code false} si el pago u otra transición se adelantó. */
    @Transactional
    public boolean expirar(UUID pedidoId) {
        Optional<Pedido> expirado = transicionar(pedidoId, EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EXPIRADO);
        if (expirado.isEmpty()) {
            return false;
        }
        Pedido p = expirado.get();
        p.agregarHistorial(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EXPIRADO, TipoActor.SISTEMA, null,
                "No se recibió el pago a tiempo");
        pedidos.save(p);
        outbox.registrar("PedidoExpirado", p.getId().toString(), new PedidoExpirado(p.getId(), p.getCodigo()));
        return true;
    }

    // ---------- entrega (delivery) ----------

    /** {@code EntregaActualizada}: en ruta o enviada → ENVIADO; entregada → ENTREGADO; fallida se anota; devuelta → DEVUELTO. */
    @Transactional
    public void aplicarEntrega(EntregaActualizada evento) {
        Optional<Pedido> existente = pedidos.findById(evento.pedidoId());
        if (existente.isEmpty()) {
            LOG.warn("EntregaActualizada de un pedido desconocido: {}", evento.pedidoId());
            return;
        }
        Pedido pedido = existente.get();
        switch (evento.estado()) {
            case "EN_RUTA", "ENVIADO_AGENCIA" -> enviar(pedido, evento);
            case "ENTREGADA" -> pasarPorEntrega(pedido, EstadoPedido.ENTREGADO, null);
            case "DEVUELTA" -> pasarPorEntrega(pedido, EstadoPedido.DEVUELTO, evento.observacion());
            case "FALLIDA" -> anotarIntentoFallido(pedido, evento);
            default -> LOG.debug("Entrega {} sin efecto en el pedido {}", evento.estado(), pedido.getCodigo());
        }
    }

    private void enviar(Pedido pedido, EntregaActualizada evento) {
        String detalle = evento.agenciaNombre() == null ? "En camino"
                : "Enviado por " + evento.agenciaNombre()
                        + (evento.codigoSeguimiento() == null ? "" : ", código " + evento.codigoSeguimiento());
        Pedido p = pedido;
        if (pedido.getEstado() == EstadoPedido.LISTO_DESPACHO) {
            maquina.validar(EstadoPedido.LISTO_DESPACHO, EstadoPedido.ENVIADO, TipoActor.SISTEMA, pedido.getModalidad());
            Optional<Pedido> enviado = transicionar(pedido.getId(), EstadoPedido.LISTO_DESPACHO, EstadoPedido.ENVIADO);
            if (enviado.isEmpty()) {
                return;
            }
            p = enviado.get();
            p.agregarHistorial(EstadoPedido.LISTO_DESPACHO, EstadoPedido.ENVIADO, TipoActor.SISTEMA, null,
                    recortar(detalle));
        } else if (pedido.getEstado() != EstadoPedido.ENVIADO) {
            LOG.debug("El pedido {} está {}; la entrega en ruta no lo cambia", pedido.getCodigo(), pedido.getEstado());
            return;
        }
        if (evento.agenciaNombre() != null) {
            p.getEnvio().setAgenciaNombre(evento.agenciaNombre());
        }
        if (evento.codigoSeguimiento() != null) {
            p.getEnvio().setCodigoSeguimiento(evento.codigoSeguimiento());
        }
        pedidos.save(p);
    }

    private void pasarPorEntrega(Pedido pedido, EstadoPedido hacia, String motivo) {
        if (pedido.getEstado() != EstadoPedido.ENVIADO) {
            LOG.debug("El pedido {} está {}; se ignora el paso a {}", pedido.getCodigo(), pedido.getEstado(), hacia);
            return;
        }
        maquina.validar(EstadoPedido.ENVIADO, hacia, TipoActor.SISTEMA, pedido.getModalidad());
        transicionar(pedido.getId(), EstadoPedido.ENVIADO, hacia).ifPresent(p -> {
            p.agregarHistorial(EstadoPedido.ENVIADO, hacia, TipoActor.SISTEMA, null, recortar(motivo));
            pedidos.save(p);
        });
    }

    private void anotarIntentoFallido(Pedido pedido, EntregaActualizada evento) {
        if (pedido.getEstado() != EstadoPedido.ENVIADO && pedido.getEstado() != EstadoPedido.LISTO_DESPACHO) {
            return;
        }
        String observacion = evento.observacion() == null ? "" : ": " + evento.observacion();
        pedido.agregarHistorial(pedido.getEstado(), pedido.getEstado(), TipoActor.SISTEMA, null,
                recortar("Intento fallido (" + evento.intento() + ")" + observacion));
        pedidos.save(pedido);
    }

    // ---------- comunes ----------

    /**
     * Cambia el estado con una actualización condicional y devuelve el pedido recargado; vacío si otra transición ganó.
     */
    Optional<Pedido> transicionar(UUID pedidoId, EstadoPedido desde, EstadoPedido hacia) {
        if (pedidos.cambiarEstadoSi(pedidoId, desde, hacia) == 0) {
            return Optional.empty();
        }
        return pedidos.findById(pedidoId);
    }

    static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= MAX_COMENTARIO ? texto : texto.substring(0, MAX_COMENTARIO);
    }
}
