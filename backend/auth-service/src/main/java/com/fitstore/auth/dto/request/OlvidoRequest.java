package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de enlace para recuperar la contraseña. */
public record OlvidoRequest(
        @NotBlank(message = Validaciones.MENSAJE_EMAIL) @Email(message = Validaciones.MENSAJE_EMAIL)
        @Size(max = 254, message = Validaciones.MENSAJE_EMAIL) String email) {
}
