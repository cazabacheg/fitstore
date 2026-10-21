package com.fitstore.auth.dto.request;

import com.fitstore.auth.model.RolStaff;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de un miembro del staff por un ADMIN: nombre y rol (el correo no se edita). {@code version} es el bloqueo optimista. */
public record EditarStaffRequest(
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String nombres,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String apellidos,
        @NotNull(message = "Elige un rol") RolStaff rol,
        @NotNull(message = "Falta la versión") Integer version) {
}
