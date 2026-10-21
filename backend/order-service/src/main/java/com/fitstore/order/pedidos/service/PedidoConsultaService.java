package com.fitstore.order.pedidos.service;

import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.order.pedidos.dto.HistorialEntradaDto;
import com.fitstore.order.pedidos.dto.PaginaPedidosAdminDto;
import com.fitstore.order.pedidos.dto.PagoPedidoDto;
import com.fitstore.order.pedidos.dto.PedidoAdminDto;
import com.fitstore.order.pedidos.dto.PedidoAdminResumenDto;
import com.fitstore.order.pedidos.dto.PedidoDetalleDto;
import com.fitstore.order.pedidos.dto.PedidoResumenDto;
import com.fitstore.order.pedidos.dto.ReembolsoDelPedidoDto;
import com.fitstore.order.pedidos.dto.ReembolsoPostventaInfoDto;
import com.fitstore.order.pedidos.dto.TransicionPermitidaDto;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.HistorialEstado;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.PedidoItem;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.repository.ContadoresPedidos;
import com.fitstore.order.pedidos.repository.PedidoReembolsoRepository;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.repository.PedidoSpecifications;
import com.fitstore.order.pedidos.service.dominio.MaquinaEstadosPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de pedidos del cliente (HU-4-06) y del backoffice (HU-4-08 y HU-4-09): listas, detalle y línea de tiempo. */
@Service
@Transactional(readOnly = true)
public class PedidoConsultaService {

    /** Los días del filtro se cuentan en hora de Lima. */
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Filtros del listado del backoffice; los nulos no filtran. */
    public record Filtros(EstadoPedido estado, Modalidad modalidad, LocalDate desde, LocalDate hasta, String codigo,
            String cliente, UUID clienteId) {
    }

    private final PedidoRepository pedidos;
    private final PedidoReembolsoRepository reembolsos;
    private final ContadoresPedidos contadores;
    private final MaquinaEstadosPedido maquina;
    private final ParametrosTienda parametros;

    public PedidoConsultaService(PedidoRepository pedidos, PedidoReembolsoRepository reembolsos,
            ContadoresPedidos contadores, MaquinaEstadosPedido maquina, ParametrosTienda parametros) {
        this.pedidos = pedidos;
        this.reembolsos = reembolsos;
        this.contadores = contadores;
        this.maquina = maquina;
        this.parametros = parametros;
    }

    // ---------- cliente ----------

    public PaginaResponse<PedidoResumenDto> misPedidos(UUID clienteId, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "creadoEn"));
        Page<PedidoResumenDto> resultado = pedidos.findByClienteId(clienteId, pageable).map(this::aResumen);
        return PaginaResponse.de(resultado);
    }

    /** Un pedido ajeno responde igual que uno inexistente (404), para no revelar que existe (CA-4-37). */
    public PedidoDetalleDto detalle(UUID clienteId, String codigo) {
        return aDetalle(propio(clienteId, codigo));
    }

    public Pedido propio(UUID clienteId, String codigo) {
        return pedidos.findByCodigo(codigo).filter(p -> p.getClienteId().equals(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pedido"));
    }

    // ---------- backoffice ----------

    public PaginaPedidosAdminDto listarAdmin(Filtros filtros, int pagina, int tamano, Sort orden) {
        Specification<Pedido> sinEstado = PedidoSpecifications.filtrar(null, filtros.modalidad(),
                inicioDelDia(filtros.desde()), inicioDelDia(filtros.hasta() == null ? null : filtros.hasta().plusDays(1)),
                filtros.codigo(), filtros.cliente(), filtros.clienteId());
        Specification<Pedido> filtro = filtros.estado() == null ? sinEstado
                : sinEstado.and(PedidoSpecifications.conEstado(filtros.estado()));
        Pageable pageable;
        if (orden.isSorted()) {
            pageable = PageRequest.of(pagina, tamano, orden);
        } else {
            pageable = PageRequest.of(pagina, tamano);
            filtro = filtro.and(PedidoSpecifications.prioridadDePreparacion());
        }
        Page<PedidoAdminResumenDto> resultado = pedidos.findAll(filtro, pageable).map(this::aResumenAdmin);
        Map<String, Long> porEstado = new LinkedHashMap<>();
        contadores.porEstado(sinEstado).forEach((estado, total) -> porEstado.put(estado.name(), total));
        return new PaginaPedidosAdminDto(resultado.getContent(), resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages(), porEstado);
    }

    public PedidoAdminDto detalleAdmin(UUID id) {
        return aAdmin(pedidos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pedido")));
    }

    public PedidoAdminDto aAdmin(Pedido p) {
        PedidoDetalleDto d = detalle(p, true);
        List<TransicionPermitidaDto> transiciones = maquina.transicionesDeStaff(p.getEstado(), p.getModalidad()).stream()
                .map(t -> new TransicionPermitidaDto(t.estado(), t.etiqueta(), t.requiereComentario(), t.requiereDni()))
                .toList();
        List<ReembolsoDelPedidoDto> hechos = reembolsos.findByPedidoIdOrderByCreadoEnAsc(p.getId()).stream()
                .map(r -> new ReembolsoDelPedidoDto(r.getId(), r.getMonto(), r.getMotivo(), r.getNota(), r.getEstado(),
                        r.getSolicitadoPor().toString(), r.getCreadoEn(), r.getCompletadoEn()))
                .toList();
        return new PedidoAdminDto(d.codigo(), d.estado(), d.modalidad(), d.creadoEn(), d.expiraEn(), d.items(),
                d.subtotal(), d.descuentoTotal(), d.costoEnvio(), d.total(), d.montoReembolsado(), d.estadoReembolso(),
                d.destinatario(), d.direccion(), d.envio(), d.notas(), d.boletaUrl(), d.pago(), d.historial(),
                d.puedeCancelar(), d.puedePagar(), p.getId(), p.getClienteId(), transiciones,
                maquina.puedeCancelar(p.getEstado(), TipoActor.STAFF),
                // El reembolso postventa es extensión de alcance (HU-4-12): por ahora nunca se permite
                new ReembolsoPostventaInfoDto(false, BigDecimal.ZERO.setScale(2), null), hechos,
                p.getVersion() == null ? 0 : p.getVersion());
    }

    // ---------- conversiones ----------

    public PedidoDetalleDto aDetalle(Pedido p) {
        return detalle(p, false);
    }

    private PedidoDetalleDto detalle(Pedido p, boolean paraStaff) {
        boolean pendiente = p.getEstado() == EstadoPedido.PENDIENTE_PAGO;
        String tienda = p.getModalidad() == Modalidad.RECOJO ? parametros.obtener().tiendaDireccion() : null;
        PagoPedidoDto pago = p.getPagoProveedor() == null ? null
                : new PagoPedidoDto(p.getPagoProveedor(), p.getPagoMedio(), p.getPagadoEn());
        return new PedidoDetalleDto(p.getCodigo(), p.getEstado(), p.getModalidad(), p.getCreadoEn(),
                pendiente ? p.getExpiraEn() : null, p.getItems().stream().map(PedidoMapper::item).toList(),
                p.getSubtotal(), p.getDescuentoTotal(), p.getCostoEnvio(), p.getTotal(), p.getMontoReembolsado(),
                PedidoMapper.estadoReembolso(p.getMontoReembolsado(), p.getTotal()),
                PedidoMapper.destinatario(p.getDestinatario()), PedidoMapper.direccion(p.getDireccion()),
                PedidoMapper.envio(p, tienda), p.getNotas(), p.getBoletaUrl(), pago,
                p.getHistorial().stream().map(h -> aEntrada(h, paraStaff)).toList(),
                maquina.puedeCancelar(p.getEstado(), TipoActor.CLIENTE),
                pendiente && p.getExpiraEn().isAfter(Instant.now()));
    }

    private PedidoResumenDto aResumen(Pedido p) {
        boolean pendiente = p.getEstado() == EstadoPedido.PENDIENTE_PAGO;
        String miniatura = p.getItems().isEmpty() ? null : p.getItems().get(0).getImagenUrl();
        return new PedidoResumenDto(p.getCodigo(), p.getCreadoEn(), p.getEstado(), p.getTotal(), p.getModalidad(),
                p.getItems().stream().mapToInt(PedidoItem::getCantidad).sum(), miniatura,
                pendiente ? p.getExpiraEn() : null, PedidoMapper.estadoReembolso(p.getMontoReembolsado(), p.getTotal()));
    }

    private PedidoAdminResumenDto aResumenAdmin(Pedido p) {
        return new PedidoAdminResumenDto(p.getId(), p.getCodigo(), p.getCreadoEn(),
                p.getDestinatario().getNombres() + " " + p.getDestinatario().getApellidos(), p.getModalidad(),
                p.getEnvio() == null ? null : p.getEnvio().getAgenciaNombre(), p.getEstado(), p.getTotal(),
                PedidoMapper.estadoReembolso(p.getMontoReembolsado(), p.getTotal()));
    }

    /**
     * Una fila del historial. Si el estado no cambió es un aviso: pago rechazado, intento fallido o reembolso; el resto
     * son cambios de estado.
     */
    private static HistorialEntradaDto aEntrada(HistorialEstado h, boolean paraStaff) {
        String comentario = h.getComentario();
        String tipo = "CAMBIO_ESTADO";
        if (h.getEstadoAnterior() != null && h.getEstadoAnterior() == h.getEstadoNuevo() && comentario != null) {
            if (comentario.startsWith("Pago rechazado")) {
                tipo = "PAGO_RECHAZADO";
            } else if (comentario.startsWith("Intento fallido")) {
                tipo = "INTENTO_FALLIDO";
            } else if (comentario.startsWith("Reembolso postventa solicitado")) {
                tipo = "REEMBOLSO_SOLICITADO";
            } else if (comentario.startsWith("Reembolso postventa completado")) {
                tipo = "REEMBOLSO_COMPLETADO";
            }
        }
        return new HistorialEntradaDto(h.getCreadoEn(), tipo,
                "CAMBIO_ESTADO".equals(tipo) ? h.getEstadoNuevo() : null, comentario, h.getActorTipo(),
                paraStaff ? h.getActorNombre() : null);
    }

    private static Instant inicioDelDia(LocalDate dia) {
        return dia == null ? null : dia.atStartOfDay(LIMA).toInstant();
    }
}
