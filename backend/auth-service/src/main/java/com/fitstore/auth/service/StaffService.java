package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.EditarStaffRequest;
import com.fitstore.auth.dto.request.InvitarStaffRequest;
import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.model.EstadoStaff;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.common.api.PaginaResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Perfil propio del staff y administración del personal (micro 4.6). */
public interface StaffService {

    StaffResponse miPerfil(UUID staffId);

    /** Actualiza nombres y apellidos; el correo y el rol no los edita el propio staff. */
    StaffResponse actualizarMiPerfil(UUID staffId, PerfilStaffRequest solicitud);

    PaginaResponse<StaffResponse> listar(String texto, RolStaff rol, EstadoStaff estado, Pageable pagina);

    StaffResponse obtener(UUID id);

    /** Crea al miembro sin contraseña y le publica la invitación (24 h). */
    StaffResponse invitar(UUID creadoPor, InvitarStaffRequest solicitud);

    /**
     * Cambia nombres, apellidos y rol (el correo no se edita).
     *
     * @throws com.fitstore.common.api.ConflictoNegocioException {@code ULTIMO_ADMIN} si degrada al último ADMIN activo;
     *         {@code CONFLICTO_VERSION} si otro usuario lo modificó antes
     */
    StaffResponse editar(UUID id, EditarStaffRequest solicitud);

    /**
     * Borrado lógico: no puede ingresar ni refrescar su sesión.
     *
     * @throws com.fitstore.common.api.ConflictoNegocioException {@code AUTO_DESACTIVACION} o {@code ULTIMO_ADMIN}
     */
    StaffResponse desactivar(UUID id, UUID solicitanteId);

    StaffResponse activar(UUID id);

    /** Nuevo enlace de invitación; solo para invitaciones pendientes. */
    void reenviarInvitacion(UUID id);
}
