package com.fitstore.auth.dto.response;

import com.fitstore.auth.model.EstadoStaff;
import com.fitstore.auth.model.RolStaff;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Miembro del staff. {@code version} es el bloqueo optimista que se devuelve al editar. */
public record StaffResponse(
        @NotNull UUID id,
        @NotNull String email,
        @NotNull String nombres,
        @NotNull String apellidos,
        @NotNull RolStaff rol,
        @NotNull EstadoStaff estado,
        Instant ultimoLoginEn,
        Instant creadoEn,
        @NotNull Integer version) {
}
