package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el cliente puede editar de su perfil: el correo y el DNI no (CA-1-15). */
public record PerfilClienteRequest(
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String nombres,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String apellidos,
        @NotNull(message = Validaciones.MENSAJE_CELULAR)
        @Pattern(regexp = Validaciones.CELULAR, message = Validaciones.MENSAJE_CELULAR) String telefono) {
}
