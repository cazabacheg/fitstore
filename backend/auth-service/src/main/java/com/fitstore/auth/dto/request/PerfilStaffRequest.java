package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que el staff puede editar de su propio perfil: el correo y el rol no (CA-1-35). */
public record PerfilStaffRequest(
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String nombres,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String apellidos) {
}
