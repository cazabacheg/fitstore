package com.fitstore.payment.service;

import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.payment.dto.BoletaInfo;
import com.fitstore.payment.dto.ClientePago;
import com.fitstore.payment.dto.PagoDetalle;
import com.fitstore.payment.dto.PagoResumen;
import com.fitstore.payment.dto.ReembolsoDto;
import com.fitstore.payment.dto.TransaccionDto;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Reembolso;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.repository.BoletaRepository;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.ReembolsoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPagoServiceImpl implements AdminPagoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<EstadoReembolso> CUENTAN_COMO_REEMBOLSADO =
            EnumSet.of(EstadoReembolso.COMPLETADO, EstadoReembolso.RESUELTO_MANUAL);
    private static final Set<EstadoIntencion> COBRADAS =
            EnumSet.of(EstadoIntencion.PAGADA, EstadoIntencion.REEMBOLSADA_PARCIAL, EstadoIntencion.REEMBOLSADA);

    private final IntencionPagoRepository intenciones;
    private final TransaccionRepository transacciones;
    private final ReembolsoRepository reembolsos;
    private final BoletaRepository boletas;

    public AdminPagoServiceImpl(IntencionPagoRepository intenciones, TransaccionRepository transacciones,
            ReembolsoRepository reembolsos, BoletaRepository boletas) {
        this.intenciones = intenciones;
        this.transacciones = transacciones;
        this.reembolsos = reembolsos;
        this.boletas = boletas;
    }

    @Override
    public Page<PagoResumen> listar(FiltroPagos filtro, Pageable paginacion) {
        Page<IntencionPago> pagina = intenciones.findAll(especificacion(filtro), paginacion);
        List<UUID> ids = pagina.getContent().stream().map(IntencionPago::getId).toList();
        Map<UUID, List<Transaccion>> intentos = transacciones.findByIntencionIdIn(ids).stream()
                .collect(Collectors.groupingBy(Transaccion::getIntencionId));
        Map<UUID, List<Reembolso>> devoluciones = reembolsos.findByIntencionIdIn(ids).stream()
                .collect(Collectors.groupingBy(Reembolso::getIntencionId));
        return pagina.map(i -> resumen(i, intentos.getOrDefault(i.getId(), List.of()),
                devoluciones.getOrDefault(i.getId(), List.of())));
    }

    @Override
    public PagoDetalle obtener(UUID intencionId) {
        IntencionPago i = intenciones.findById(intencionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pago"));
        List<Transaccion> intentos = transacciones.findByIntencionIdOrderByCreadoEnAsc(i.getId());
        List<Reembolso> devoluciones = reembolsos.findByIntencionId(i.getId());
        PagoResumen r = resumen(i, intentos, devoluciones);
        BigDecimal reembolsado = devoluciones.stream().filter(d -> CUENTAN_COMO_REEMBOLSADO.contains(d.getEstado()))
                .map(Reembolso::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cobrado = COBRADAS.contains(i.getEstado()) ? i.getMonto() : BigDecimal.ZERO;
        return new PagoDetalle(r.id(), r.codigoPedido(), r.clienteNombre(), r.monto(), r.proveedor(), r.medio(),
                r.intentos(), r.estado(), r.creadoEn(), r.tieneReembolsoFallido(), i.getPedidoId(),
                new ClientePago(texto(i.getCliente().get("nombre")), texto(i.getCliente().get("dni")),
                        texto(i.getCliente().get("email"))),
                intentos.stream().map(AdminPagoServiceImpl::transaccion).toList(),
                devoluciones.stream().map(d -> reembolso(d, i)).toList(),
                boletas.findByIntencionId(i.getId()).map(b -> new BoletaInfo(b.getNumero(), b.getEmitidaEn())).orElse(null),
                reembolsado, cobrado.subtract(reembolsado));
    }

    private static PagoResumen resumen(IntencionPago i, List<Transaccion> intentos, List<Reembolso> devoluciones) {
        Transaccion aprobada = intentos.stream().filter(t -> t.getEstado() == EstadoTransaccion.APROBADA).findFirst().orElse(null);
        return new PagoResumen(i.getId(), i.getCodigoPedido(), texto(i.getCliente().get("nombre")), i.getMonto(),
                aprobada == null ? null : aprobada.getProveedor(), aprobada == null ? null : aprobada.getMedio(),
                intentos.size(), i.getEstado(), i.getCreadoEn(),
                devoluciones.stream().anyMatch(d -> d.getEstado() == EstadoReembolso.FALLIDO));
    }

    private static TransaccionDto transaccion(Transaccion t) {
        return new TransaccionDto(t.getId(), t.getProveedor(), t.getMedio(), t.getEstado(), t.getMonto(),
                t.getIdProveedor(), t.getCodigoRespuesta(), t.getMensaje(), t.getCreadoEn());
    }

    private static ReembolsoDto reembolso(Reembolso d, IntencionPago i) {
        return new ReembolsoDto(d.getId(), d.getIntencionId(), i.getCodigoPedido(), texto(i.getCliente().get("nombre")),
                d.getMonto(), d.getMotivo(), d.getEstado(), d.getIntentos(), d.getUltimoError(), d.getProximoIntentoEn(),
                d.getResueltoPor(), d.getNota(), d.getCreadoEn());
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static Specification<IntencionPago> especificacion(FiltroPagos f) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (f.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), f.estado()));
            }
            if (f.proveedor() != null) {
                Subquery<UUID> intento = consulta.subquery(UUID.class);
                var t = intento.from(Transaccion.class);
                intento.select(t.get("id")).where(cb.equal(t.get("intencionId"), raiz.get("id")),
                        cb.equal(t.get("proveedor"), f.proveedor()));
                condiciones.add(cb.exists(intento));
            }
            if (f.codigo() != null && !f.codigo().isBlank()) {
                condiciones.add(cb.like(cb.lower(raiz.get("codigoPedido")),
                        "%" + f.codigo().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (f.desde() != null) {
                Instant inicio = f.desde().atStartOfDay(LIMA).toInstant();
                condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("creadoEn"), inicio));
            }
            if (f.hasta() != null) {
                Instant fin = f.hasta().plusDays(1).atStartOfDay(LIMA).toInstant();
                condiciones.add(cb.lessThan(raiz.<Instant>get("creadoEn"), fin));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
