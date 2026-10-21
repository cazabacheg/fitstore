package com.fitstore.delivery.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.delivery.config.ManejadorErroresDelivery;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.AsignarRequest;
import com.fitstore.delivery.dto.EntregaDto;
import com.fitstore.delivery.dto.EntregaResumenDto;
import com.fitstore.delivery.dto.EnvioAgenciaRequest;
import com.fitstore.delivery.dto.FiltroEntregas;
import com.fitstore.delivery.dto.ObservacionRequest;
import com.fitstore.delivery.dto.PaginaEntregasDto;
import com.fitstore.delivery.dto.VersionRequest;
import com.fitstore.delivery.event.EntregaActualizada;
import com.fitstore.delivery.event.PedidoListoDespacho;
import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Destinatario;
import com.fitstore.delivery.model.DireccionEnvio;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EntregaEvento;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.repository.AgenciaRepository;
import com.fitstore.delivery.repository.EntregaEventoRepository;
import com.fitstore.delivery.repository.EntregaRepository;
import com.fitstore.delivery.repository.EntregaSpecifications;
import com.fitstore.delivery.repository.RepartidorRepository;
import com.fitstore.delivery.service.dominio.MaquinaEstadosEntrega;
import com.fitstore.delivery.service.dominio.MaquinaEstadosEntrega.DatosAvance;
import com.fitstore.delivery.service.dominio.UrlRastreo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entregas: se crean desde {@code PedidoListoDespacho} y el operador las lleva hasta el final (CU-26 y CU-26b). Cada
 * avance pasa por {@link MaquinaEstadosEntrega}, deja un {@link EntregaEvento} de seguimiento y registra
 * {@code EntregaActualizada} en el outbox, todo en la misma transacción (ADR-009).
 */
@Service
@Transactional
public class EntregaService {

    private final EntregaRepository entregas;
    private final EntregaEventoRepository seguimiento;
    private final RepartidorRepository repartidores;
    private final AgenciaRepository agencias;
    private final OutboxRegistrador outbox;
    private final Clock reloj;

    public EntregaService(EntregaRepository entregas, EntregaEventoRepository seguimiento,
            RepartidorRepository repartidores, AgenciaRepository agencias, OutboxRegistrador outbox, Clock reloj) {
        this.entregas = entregas;
        this.seguimiento = seguimiento;
        this.repartidores = repartidores;
        this.agencias = agencias;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    // --- Creación (RF-DEL-01) ---

    /** Idempotente por {@code pedidoId}: si la entrega ya existe, devuelve esa y no publica nada (CA-6-19). */
    public EntregaDto crearDesde(PedidoListoDespacho pedido) {
        Optional<Entrega> existente = entregas.findByPedidoId(pedido.pedidoId());
        if (existente.isPresent()) {
            return aDto(existente.get());
        }
        Modalidad modalidad = Modalidad.valueOf(pedido.modalidad());
        Agencia agencia = modalidad == Modalidad.AGENCIA && pedido.agenciaId() != null
                ? agencias.findById(pedido.agenciaId()).orElse(null) : null;
        PedidoListoDespacho.Destinatario d = pedido.destinatario();
        PedidoListoDespacho.Direccion dir = pedido.direccion();
        Entrega entrega = Entrega.builder()
                .pedidoId(pedido.pedidoId())
                .codigoPedido(pedido.codigo())
                .modalidad(modalidad)
                .estado(MaquinaEstadosEntrega.estadoInicial(modalidad))
                .destinatario(Destinatario.builder().nombres(d.nombres()).apellidos(d.apellidos()).dni(d.dni())
                        .telefono(d.telefono()).email(d.email()).build())
                .direccion(DireccionEnvio.builder().ubigeo(dir.ubigeo()).departamento(dir.departamento())
                        .provincia(dir.provincia()).distrito(dir.distrito()).direccion(dir.direccion())
                        .referencia(dir.referencia()).build())
                .agencia(agencia)
                .resumenItems(pedido.resumenItems())
                .bultos(Math.max(1, pedido.bultos()))
                .build();
        entregas.saveAndFlush(entrega);
        registrar(entrega, null, null);
        return aDto(entrega);
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public EntregaDto obtener(UUID id) {
        return aDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public PaginaEntregasDto listar(FiltroEntregas filtro, Pageable paginacion) {
        Specification<Entrega> spec = EntregaSpecifications.filtrar(filtro, true);
        if (paginacion.getSort().isUnsorted()) {
            spec = spec.and(EntregaSpecifications.pendientesPrimero());
        }
        Page<EntregaResumenDto> pagina = entregas.findAll(spec, paginacion).map(EntregaResumenDto::de);
        Map<EstadoEntrega, Long> cuentas = entregas.contarPorEstado(EntregaSpecifications.filtrar(filtro, false));
        Map<String, Long> contadores = new LinkedHashMap<>();
        for (EstadoEntrega estado : EstadoEntrega.values()) {
            contadores.put(estado.name(), cuentas.getOrDefault(estado, 0L));
        }
        return new PaginaEntregasDto(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages(), contadores);
    }

    // --- Avances del delivery local ---

    /** Asigna o reprograma (CA-6-20): desde PENDIENTE_ASIGNACION, ASIGNADA o FALLIDA (esta última suma un intento). */
    public EntregaDto asignar(UUID id, AsignarRequest pedido, Actor actor) {
        Entrega entrega = abrir(id, pedido.version());
        MaquinaEstadosEntrega.validar(entrega.getModalidad(), entrega.getEstado(), EstadoEntrega.ASIGNADA,
                new DatosAvance(null, null));
        Repartidor repartidor = repartidores.findById(pedido.repartidorId()).filter(Repartidor::isActivo)
                .orElseThrow(() -> new SolicitudInvalidaException("repartidorId", "Elige un repartidor activo"));
        if (pedido.fechaProgramada().isBefore(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException("fechaProgramada", "La fecha debe ser hoy o posterior");
        }
        EstadoEntrega desde = entrega.getEstado();
        return aplicar(entrega, EstadoEntrega.ASIGNADA, null, actor, e -> {
            e.setRepartidor(repartidor);
            e.setFechaProgramada(pedido.fechaProgramada());
            if (desde == EstadoEntrega.PENDIENTE_ASIGNACION) {
                e.setIntentos(1);
            } else if (desde == EstadoEntrega.FALLIDA) {
                e.setIntentos(e.getIntentos() + 1);
            }
        });
    }

    public EntregaDto enRuta(UUID id, VersionRequest pedido, Actor actor) {
        return avanzar(id, pedido.version(), EstadoEntrega.EN_RUTA, new DatosAvance(null, null), actor, e -> { });
    }

    /** Entregada (local) o recogida por el cliente en la agencia (CA-6-21 y CA-6-27). */
    public EntregaDto entregada(UUID id, VersionRequest pedido, Actor actor) {
        return avanzar(id, pedido.version(), EstadoEntrega.ENTREGADA, new DatosAvance(null, null), actor, e -> { });
    }

    public EntregaDto fallida(UUID id, ObservacionRequest pedido, Actor actor) {
        String observacion = pedido.observacion() == null ? null : pedido.observacion().trim();
        return avanzar(id, pedido.version(), EstadoEntrega.FALLIDA, new DatosAvance(observacion, null), actor, e -> { });
    }

    // --- Avances por agencia ---

    public EntregaDto enviadaAgencia(UUID id, EnvioAgenciaRequest pedido, Actor actor) {
        String codigo = pedido.codigoSeguimiento() == null ? null : pedido.codigoSeguimiento().trim();
        Entrega previa = abrir(id, pedido.version());
        MaquinaEstadosEntrega.validar(previa.getModalidad(), previa.getEstado(), EstadoEntrega.ENVIADO_AGENCIA,
                new DatosAvance(null, codigo));
        Agencia otra = pedido.agenciaId() == null ? null : agencias.findById(pedido.agenciaId()).filter(Agencia::isActiva)
                .orElseThrow(() -> new SolicitudInvalidaException("agenciaId", "Elige una agencia disponible"));
        return aplicar(previa, EstadoEntrega.ENVIADO_AGENCIA, null, actor, e -> {
            if (otra != null) {
                e.setAgencia(otra);
            }
            e.setCodigoSeguimiento(codigo);
            e.setClaveRecojo(pedido.claveRecojo() == null || pedido.claveRecojo().isBlank() ? null
                    : pedido.claveRecojo().trim());
        });
    }

    public EntregaDto disponibleAgencia(UUID id, VersionRequest pedido, Actor actor) {
        return avanzar(id, pedido.version(), EstadoEntrega.DISPONIBLE_EN_AGENCIA, new DatosAvance(null, null), actor,
                e -> { });
    }

    // --- Mecánica común ---

    private EntregaDto avanzar(UUID id, int version, EstadoEntrega hacia, DatosAvance datos, Actor actor,
            Consumer<Entrega> cambio) {
        Entrega entrega = abrir(id, version);
        MaquinaEstadosEntrega.validar(entrega.getModalidad(), entrega.getEstado(), hacia, datos);
        return aplicar(entrega, hacia, datos.observacion(), actor, cambio);
    }

    /** Busca la entrega y comprueba que el operador la vio en su versión actual (E6, CA-6-24). */
    private Entrega abrir(UUID id, int version) {
        Entrega entrega = buscar(id);
        if (entrega.getVersion() == null || entrega.getVersion() != version) {
            throw new ConflictoNegocioException("CONFLICTO_VERSION", ManejadorErroresDelivery.MENSAJE_VERSION);
        }
        return entrega;
    }

    private EntregaDto aplicar(Entrega entrega, EstadoEntrega hacia, String observacion, Actor actor,
            Consumer<Entrega> cambio) {
        cambio.accept(entrega);
        entrega.setEstado(hacia);
        entregas.saveAndFlush(entrega);
        registrar(entrega, observacion, actor);
        return aDto(entrega);
    }

    /** Deja el paso en el seguimiento y publica {@code EntregaActualizada} (sin el teléfono del repartidor, D6). */
    private void registrar(Entrega entrega, String observacion, Actor actor) {
        seguimiento.save(EntregaEvento.builder()
                .entrega(entrega)
                .estado(entrega.getEstado())
                .observacion(observacion)
                .actorId(actor == null ? null : actor.id())
                .actorNombre(actor == null ? null : actor.nombre())
                .intento(entrega.getIntentos())
                .creadoEn(Instant.now(reloj))
                .build());
        outbox.registrar("EntregaActualizada", entrega.getId().toString(), new EntregaActualizada(
                entrega.getId(), entrega.getPedidoId(), entrega.getCodigoPedido(), entrega.getModalidad().name(),
                entrega.getEstado().name(), entrega.getIntentos(), entrega.getFechaProgramada(),
                entrega.getRepartidor() == null ? null : entrega.getRepartidor().getNombres(),
                entrega.getAgencia() == null ? null : entrega.getAgencia().getNombre(),
                entrega.getCodigoSeguimiento(), entrega.getClaveRecojo(),
                UrlRastreo.armar(entrega.getAgencia(), entrega.getCodigoSeguimiento()), observacion,
                new EntregaActualizada.Persona(
                        entrega.getDestinatario().getNombres() + " " + entrega.getDestinatario().getApellidos(),
                        entrega.getDestinatario().getEmail())));
    }

    private Entrega buscar(UUID id) {
        return entregas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa entrega"));
    }

    private EntregaDto aDto(Entrega entrega) {
        return EntregaDto.de(entrega, seguimiento.findByEntregaIdOrderByCreadoEnAscIdAsc(entrega.getId()));
    }
}
