package com.fitstore.order.pedidos.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.order.event.BoletaEmitida;
import com.fitstore.order.event.EntregaActualizada;
import com.fitstore.order.event.PagoConfirmado;
import com.fitstore.order.event.PagoNoAplicable;
import com.fitstore.order.event.PagoRechazado;
import com.fitstore.order.event.PedidoCancelado;
import com.fitstore.order.event.PedidoExpirado;
import com.fitstore.order.event.PedidoListoDespacho;
import com.fitstore.order.event.PedidoListoRecojo;
import com.fitstore.order.event.PedidoPagado;
import com.fitstore.order.event.PedidoRecogido;
import com.fitstore.order.pedidos.dto.AvanzarEstadoRequest;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.service.dominio.MaquinaEstadosPedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    /** Quién hace un cambio: el cliente, un miembro del staff (con su nombre del JWT) o el sistema. */
    public record Actor(TipoActor tipo, UUID id, String nombre) {
    }

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

    // ---------- cancelación (4.6) ----------

    /** El cliente cancela su pedido en PENDIENTE_PAGO o PAGADO; uno ajeno responde 404. */
    @Transactional
    public Pedido cancelarComoCliente(UUID clienteId, String codigo) {
        Pedido pedido = pedidos.findByCodigo(codigo).filter(p -> p.getClienteId().equals(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pedido"));
        return cancelar(pedido, new Actor(TipoActor.CLIENTE, clienteId, null), "Cancelado por el cliente");
    }

    /** El staff cancela hasta EN_PREPARACION con un motivo; {@code version} es la que vio el operador (O5). */
    @Transactional
    public Pedido cancelarComoStaff(UUID pedidoId, long version, String motivo, Actor actor) {
        Pedido pedido = buscar(pedidoId);
        verificarVersion(pedido, version);
        return cancelar(pedido, actor, motivo.trim());
    }

    private Pedido cancelar(Pedido pedido, Actor actor, String motivo) {
        EstadoPedido desde = pedido.getEstado();
        if (!maquina.puedeCancelar(desde, actor.tipo())) {
            throw new ConflictoNegocioException("NO_CANCELABLE",
                    actor.tipo() == TipoActor.CLIENTE ? "El pedido ya está en preparación"
                            : "El pedido ya no se puede cancelar",
                    Map.<String, Object>of("estadoActual", desde.name()));
        }
        Pedido cancelado = transicionar(pedido.getId(), desde, EstadoPedido.CANCELADO)
                .orElseThrow(PedidoService::conflictoDeVersion);
        cancelado.agregarHistorial(desde, EstadoPedido.CANCELADO, actor.tipo(), actor.id(), actor.nombre(), motivo);
        pedidos.save(cancelado);
        outbox.registrar("PedidoCancelado", cancelado.getId().toString(),
                new PedidoCancelado(cancelado.getId(), cancelado.getCodigo(), desde != EstadoPedido.PENDIENTE_PAGO,
                        motivo, actor.tipo().name(), PedidoMapper.cantidades(cancelado)));
        return cancelado;
    }

    // ---------- preparación y despacho (4.5) ----------

    /** El staff avanza el pedido: preparar, dejarlo listo para despacho o recojo, entregarlo en tienda o devolverlo. */
    @Transactional
    public Pedido avanzar(UUID pedidoId, AvanzarEstadoRequest peticion, Actor actor) {
        Pedido pedido = buscar(pedidoId);
        verificarVersion(pedido, peticion.version());
        EstadoPedido desde = pedido.getEstado();
        EstadoPedido hacia = peticion.nuevoEstado();
        maquina.validar(desde, hacia, TipoActor.STAFF, pedido.getModalidad());
        if (maquina.transicionesDeStaff(desde, pedido.getModalidad()).stream().noneMatch(t -> t.estado() == hacia)) {
            // Cancelar tiene su propio endpoint (con motivo obligatorio)
            throw new ConflictoNegocioException("TRANSICION_INVALIDA",
                    "No se puede pasar el pedido de " + desde + " a " + hacia,
                    Map.<String, Object>of("estadoActual", desde.name()));
        }
        if (hacia == EstadoPedido.ENTREGADO && !Boolean.TRUE.equals(peticion.dniVerificado())) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION",
                    "Confirma que verificaste el DNI del destinatario");
        }
        String comentario = peticion.comentario() == null || peticion.comentario().isBlank() ? null
                : recortar(peticion.comentario().trim());
        if (hacia == EstadoPedido.DEVUELTO && comentario == null) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "Indica el motivo");
        }
        Pedido avanzado = transicionar(pedido.getId(), desde, hacia).orElseThrow(PedidoService::conflictoDeVersion);
        avanzado.agregarHistorial(desde, hacia, actor.tipo(), actor.id(), actor.nombre(), comentario);
        pedidos.save(avanzado);
        publicarAvance(avanzado, hacia);
        return avanzado;
    }

    private void publicarAvance(Pedido p, EstadoPedido hacia) {
        String id = p.getId().toString();
        switch (hacia) {
            case LISTO_DESPACHO -> outbox.registrar("PedidoListoDespacho", id, new PedidoListoDespacho(p.getId(),
                    p.getCodigo(), p.getModalidad(), PedidoMapper.destinatarioEvento(p.getDestinatario()),
                    PedidoMapper.direccionEvento(p.getDireccion()), p.getEnvio().getAgenciaId(),
                    p.getEnvio().getAgenciaNombre(), resumenDeItems(p), 1));
            case LISTO_RECOJO -> outbox.registrar("PedidoListoRecojo", id,
                    new PedidoListoRecojo(p.getId(), p.getCodigo(), PedidoMapper.destinatarioEvento(p.getDestinatario())));
            case ENTREGADO -> outbox.registrar("PedidoRecogido", id,
                    new PedidoRecogido(p.getId(), p.getCodigo(), PedidoMapper.destinatarioEvento(p.getDestinatario())));
            default -> { }
        }
    }

    private static String resumenDeItems(Pedido p) {
        return p.getItems().stream()
                .map(i -> i.getCantidad() + " × " + i.getProductoNombre() + " " + i.getTalla() + " " + i.getColor())
                .collect(Collectors.joining("; "));
    }

    private Pedido buscar(UUID pedidoId) {
        return pedidos.findById(pedidoId).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pedido"));
    }

    private static void verificarVersion(Pedido pedido, long version) {
        if (pedido.getVersion() == null || pedido.getVersion() != version) {
            throw conflictoDeVersion();
        }
    }

    static ConflictoNegocioException conflictoDeVersion() {
        return new ConflictoNegocioException("CONFLICTO_VERSION", "El pedido cambió; recarga la página");
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
