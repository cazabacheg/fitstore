package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.EditarStaffRequest;
import com.fitstore.auth.dto.request.InvitarStaffRequest;
import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.mapper.StaffMapper;
import com.fitstore.auth.model.EstadoStaff;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.StaffRepository;
import com.fitstore.auth.repository.StaffSpecs;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository personal;
    private final StaffMapper mapper;
    private final PasswordService passwords;
    private final Clock reloj;

    public StaffServiceImpl(StaffRepository personal, StaffMapper mapper, PasswordService passwords, Clock reloj) {
        this.personal = personal;
        this.mapper = mapper;
        this.passwords = passwords;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse miPerfil(UUID staffId) {
        return mapper.aRespuesta(buscar(staffId));
    }

    @Override
    @Transactional
    public StaffResponse actualizarMiPerfil(UUID staffId, PerfilStaffRequest solicitud) {
        Staff staff = buscar(staffId);
        mapper.actualizar(solicitud, staff);
        return mapper.aRespuesta(personal.saveAndFlush(staff));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<StaffResponse> listar(String texto, RolStaff rol, EstadoStaff estado, Pageable pagina) {
        return PaginaResponse.de(personal.findAll(StaffSpecs.filtrar(texto, rol, estado), pagina).map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse obtener(UUID id) {
        return mapper.aRespuesta(buscar(id));
    }

    @Override
    @Transactional
    public StaffResponse invitar(UUID creadoPor, InvitarStaffRequest solicitud) {
        String email = Correo.normalizar(solicitud.email());
        if (personal.existsByEmailIgnoreCase(email)) {
            throw new ConflictoNegocioException("CORREO_DUPLICADO", "El correo ya está registrado");
        }
        Staff nuevo = new Staff();
        nuevo.setEmail(email);
        nuevo.setNombres(solicitud.nombres().trim());
        nuevo.setApellidos(solicitud.apellidos().trim());
        nuevo.setRol(solicitud.rol());
        nuevo.setActivo(true);
        nuevo.setCreadoPor(creadoPor);
        nuevo.setCredencialesActualizadasEn(reloj.instant().truncatedTo(ChronoUnit.MILLIS));
        personal.saveAndFlush(nuevo);
        passwords.enviarInvitacion(nuevo);
        return mapper.aRespuesta(nuevo);
    }

    @Override
    @Transactional
    public StaffResponse editar(UUID id, EditarStaffRequest solicitud) {
        Staff staff = buscar(id);
        if (!Objects.equals(solicitud.version(), staff.getVersion())) {
            throw conflictoDeVersion();
        }
        if (staff.getRol() == RolStaff.ADMIN && solicitud.rol() != RolStaff.ADMIN && esAdminOperativo(staff) && esElUltimoAdmin()) {
            throw ultimoAdmin();
        }
        staff.setNombres(solicitud.nombres().trim());
        staff.setApellidos(solicitud.apellidos().trim());
        staff.setRol(solicitud.rol());
        return mapper.aRespuesta(personal.saveAndFlush(staff));
    }

    @Override
    @Transactional
    public StaffResponse desactivar(UUID id, UUID solicitanteId) {
        if (id.equals(solicitanteId)) {
            throw new ConflictoNegocioException("AUTO_DESACTIVACION", "No puedes desactivar tu propia cuenta");
        }
        Staff staff = buscar(id);
        if (esAdminOperativo(staff) && esElUltimoAdmin()) {
            throw ultimoAdmin();
        }
        staff.setActivo(false);
        // invalida sus sesiones: ya no puede refrescar (invariantes 5 y 8)
        staff.setCredencialesActualizadasEn(reloj.instant().truncatedTo(ChronoUnit.MILLIS));
        return mapper.aRespuesta(personal.saveAndFlush(staff));
    }

    @Override
    @Transactional
    public StaffResponse activar(UUID id) {
        Staff staff = buscar(id);
        staff.setActivo(true);
        return mapper.aRespuesta(personal.saveAndFlush(staff));
    }

    @Override
    @Transactional
    public void reenviarInvitacion(UUID id) {
        Staff staff = buscar(id);
        if (staff.estado() != EstadoStaff.INVITACION_PENDIENTE) {
            throw new ConflictoNegocioException("INVITACION_NO_PENDIENTE", "Solo se puede reenviar a una invitación pendiente");
        }
        passwords.enviarInvitacion(staff);
    }

    /** Un ADMIN que hoy puede ingresar: activo y con contraseña. */
    private static boolean esAdminOperativo(Staff staff) {
        return staff.getRol() == RolStaff.ADMIN && staff.isActivo() && staff.getPasswordHash() != null;
    }

    /** Invariante 6: si queda un solo ADMIN operativo, no se puede desactivar ni degradar. */
    private boolean esElUltimoAdmin() {
        return personal.countByRolAndActivoTrueAndPasswordHashIsNotNull(RolStaff.ADMIN) <= 1;
    }

    private static ConflictoNegocioException ultimoAdmin() {
        return new ConflictoNegocioException("ULTIMO_ADMIN", "Debe quedar al menos un administrador activo");
    }

    private static ConflictoNegocioException conflictoDeVersion() {
        return new ConflictoNegocioException("CONFLICTO_VERSION", "Otro usuario modificó este registro; recarga la página");
    }

    private Staff buscar(UUID id) {
        return personal.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos lo que buscas"));
    }
}
