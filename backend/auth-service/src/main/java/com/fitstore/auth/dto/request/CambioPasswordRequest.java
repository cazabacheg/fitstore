package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña desde el perfil: pide la actual (CA-1-16, CA-1-35). */
public record CambioPasswordRequest(
        @NotBlank(message = "Ingresa tu contraseña actual") @Size(max = 64, message = "Ingresa tu contraseña actual") String passwordActual,
        @NotNull(message = Validaciones.MENSAJE_PASSWORD)
        @Pattern(regexp = Validaciones.PASSWORD, message = Validaciones.MENSAJE_PASSWORD) String nuevaPassword) {
}
