package com.fitstore.settings.reclamaciones.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.settings.config.PropiedadesSettings;
import com.fitstore.settings.config.SolicitudInvalidaException;
import com.fitstore.settings.event.PersonaEvento;
import com.fitstore.settings.event.ReclamoRegistrado;
import com.fitstore.settings.event.ReclamoRespondido;
import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.dto.ReclamoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRegistradoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRequest;
import com.fitstore.settings.reclamaciones.dto.ReclamoResumenDto;
import com.fitstore.settings.reclamaciones.mapper.ReclamoMapper;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import com.fitstore.settings.reclamaciones.model.TipoDocumento;
import com.fitstore.settings.reclamaciones.repository.CorrelativoReclamoRepository;
import com.fitstore.settings.reclamaciones.repository.ReclamoRepository;
import com.fitstore.settings.reclamaciones.service.dominio.CalculadoraDiasHabiles;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ReclamoServiceImpl implements ReclamoService {

    private static final int MAXIMO_IP = 45;

    private final ReclamoRepository reclamos;
    private final CorrelativoReclamoRepository correlativos;
    private final OutboxRegistrador outbox;
    private final ReclamoMapper mapper;
    private final PropiedadesSettings propiedades;
    private final Clock reloj;
    private final JsonMapper json;

    public ReclamoServiceImpl(ReclamoRepository reclamos, CorrelativoReclamoRepository correlativos,
            OutboxRegistrador outbox, ReclamoMapper mapper, PropiedadesSettings propiedades, Clock reloj,
            JsonMapper json) {
        this.reclamos = reclamos;
        this.correlativos = correlativos;
        this.outbox = outbox;
        this.mapper = mapper;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.json = json;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public ReclamoRegistradoDto registrar(ReclamoRequest solicitud, String ip) {
        validarConsumidor(solicitud);
        Instant ahora = Instant.now(reloj);
        LocalDate hoy = LocalDate.now(reloj);
        int anio = hoy.getYear();
        // Mismo número y mismo reclamo en una sola transacción: si algo falla, el número no se consume
        int correlativo = correlativos.siguiente(anio);

        Reclamo reclamo = new Reclamo();
        reclamo.setAnio(anio);
        reclamo.setCorrelativo(correlativo);
        reclamo.setNumero("%s-%d-%06d".formatted(propiedades.reclamos().serie(), anio, correlativo));
        reclamo.setTipo(solicitud.tipo());
        reclamo.setEstado(EstadoReclamo.REGISTRADO);
        reclamo.setConsumidor(mapper.aEntidad(solicitud.consumidor()));
        reclamo.setBien(mapper.aEntidad(solicitud.bien()));
        reclamo.setDetalle(solicitud.detalle());
        reclamo.setPedidoConsumidor(solicitud.pedidoConsumidor());
        reclamo.setFechaLimite(CalculadoraDiasHabiles.sumar(hoy, propiedades.reclamos().plazoDiasHabiles()));
        reclamo.setIp(ip == null ? null : ip.substring(0, Math.min(ip.length(), MAXIMO_IP)));
        reclamo.setCreadoEn(ahora);
        Reclamo guardado = reclamos.saveAndFlush(reclamo);

        outbox.registrar("ReclamoRegistrado", guardado.getNumero(), new ReclamoRegistrado(guardado.getNumero(),
                guardado.getTipo().name(), personaDe(guardado),
                json.convertValue(mapper.aDto(guardado.getBien()), Map.class), guardado.getDetalle(),
                guardado.getPedidoConsumidor(), guardado.getFechaLimite(), guardado.getCreadoEn()));
        return new ReclamoRegistradoDto(guardado.getNumero(), guardado.getFechaLimite(), guardado.getCreadoEn());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReclamoResumenDto> listar(FiltroReclamos filtro, Pageable paginacion) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate limite = limitePorVencer(hoy);
        Page<Reclamo> pagina = reclamos.buscar(filtro, hoy, limite, paginacion);
        return PaginaResponse.de(pagina.map(r -> resumen(r, hoy, limite)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReclamoDto obtener(Long id) {
        return detalle(buscar(id));
    }

    @Override
    @Transactional
    public ReclamoDto tomar(Long id) {
        Reclamo reclamo = buscarParaActualizar(id);
        if (reclamo.getEstado() != EstadoReclamo.REGISTRADO) {
            throw estadoInvalido("Solo se puede tomar un reclamo registrado");
        }
        reclamo.setEstado(EstadoReclamo.EN_ATENCION);
        return detalle(reclamos.saveAndFlush(reclamo));
    }

    @Override
    @Transactional
    public ReclamoDto responder(Long id, String respuesta, String staffId) {
        Reclamo reclamo = buscarParaActualizar(id);
        if (reclamo.getEstado() == EstadoReclamo.RESPONDIDO) {
            throw estadoInvalido("El reclamo ya fue respondido y no se puede modificar");
        }
        reclamo.setEstado(EstadoReclamo.RESPONDIDO);
        reclamo.setRespuesta(respuesta);
        reclamo.setRespondidoEn(Instant.now(reloj));
        reclamo.setRespondidoPor(staffId);
        Reclamo guardado = reclamos.saveAndFlush(reclamo);
        outbox.registrar("ReclamoRespondido", guardado.getNumero(), new ReclamoRespondido(guardado.getNumero(),
                personaDe(guardado), guardado.getRespuesta(), guardado.getRespondidoEn()));
        return detalle(guardado);
    }

    private void validarConsumidor(ReclamoRequest solicitud) {
        var consumidor = solicitud.consumidor();
        if (Boolean.TRUE.equals(consumidor.esMenor())
                && (consumidor.apoderadoNombre() == null || consumidor.apoderadoNombre().isBlank())) {
            throw new SolicitudInvalidaException("consumidor.apoderadoNombre",
                    "Indica el nombre del padre, madre o apoderado");
        }
        if (consumidor.tipoDocumento() == TipoDocumento.DNI && !consumidor.numeroDocumento().matches("[0-9]{8}")) {
            throw new SolicitudInvalidaException("consumidor.numeroDocumento", "El DNI debe tener 8 dígitos");
        }
    }

    private Reclamo buscar(Long id) {
        return reclamos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el reclamo"));
    }

    private Reclamo buscarParaActualizar(Long id) {
        return reclamos.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el reclamo"));
    }

    private static ConflictoNegocioException estadoInvalido(String mensaje) {
        return new ConflictoNegocioException("ESTADO_INVALIDO", mensaje);
    }

    private static PersonaEvento personaDe(Reclamo r) {
        return new PersonaEvento(nombreCompleto(r), r.getConsumidor().getEmail());
    }

    private static String nombreCompleto(Reclamo r) {
        return r.getConsumidor().getNombres() + " " + r.getConsumidor().getApellidos();
    }

    /** Última fecha que cuenta como "por vencer": hoy más 3 días hábiles. */
    private LocalDate limitePorVencer(LocalDate hoy) {
        return CalculadoraDiasHabiles.sumar(hoy, propiedades.reclamos().porVencerDias());
    }

    private ReclamoResumenDto resumen(Reclamo r, LocalDate hoy, LocalDate limite) {
        boolean sinResponder = r.getEstado() != EstadoReclamo.RESPONDIDO;
        boolean vencido = sinResponder && r.getFechaLimite().isBefore(hoy);
        boolean porVencer = sinResponder && !vencido && !r.getFechaLimite().isAfter(limite);
        return new ReclamoResumenDto(r.getId(), r.getNumero(), r.getTipo(), r.getEstado(), nombreCompleto(r),
                r.getCreadoEn(), r.getFechaLimite(), porVencer, vencido);
    }

    private ReclamoDto detalle(Reclamo r) {
        LocalDate hoy = LocalDate.now(reloj);
        ReclamoResumenDto base = resumen(r, hoy, limitePorVencer(hoy));
        return new ReclamoDto(base.id(), base.numero(), base.tipo(), base.estado(), base.consumidorNombre(),
                base.creadoEn(), base.fechaLimite(), base.porVencer(), base.vencido(),
                mapper.aDto(r.getConsumidor()), mapper.aDto(r.getBien()), r.getDetalle(), r.getPedidoConsumidor(),
                r.getRespuesta(), r.getRespondidoEn(), r.getRespondidoPor());
    }
}
