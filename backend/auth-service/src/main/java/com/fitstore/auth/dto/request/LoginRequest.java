package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de ingreso (cliente o staff). */
public record LoginRequest(
        @NotBlank(message = Validaciones.MENSAJE_EMAIL) @Email(message = Validaciones.MENSAJE_EMAIL)
        @Size(max = 254, message = Validaciones.MENSAJE_EMAIL) String email,
        @NotBlank(message = "Ingresa tu contraseña") @Size(max = 64, message = "Ingresa tu contraseña") String password) {
}
