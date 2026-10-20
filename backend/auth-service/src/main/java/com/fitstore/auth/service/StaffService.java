package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import java.util.UUID;

/** Perfil propio del staff y administración del personal (micro 4.6). */
public interface StaffService {

    StaffResponse miPerfil(UUID staffId);

    /** Actualiza nombres y apellidos; el correo y el rol no los edita el propio staff. */
    StaffResponse actualizarMiPerfil(UUID staffId, PerfilStaffRequest solicitud);
}
