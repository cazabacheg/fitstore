package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Nueva contraseña con el token del enlace de recuperación o de invitación. */
public record RestablecerRequest(
        @NotNull(message = "Falta el enlace") @Size(min = 20, max = 100, message = "Enlace inválido o expirado") String token,
        @NotNull(message = Validaciones.MENSAJE_PASSWORD)
        @Pattern(regexp = Validaciones.PASSWORD, message = Validaciones.MENSAJE_PASSWORD) String nuevaPassword) {
}
