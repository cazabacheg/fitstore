package com.fitstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dirección de envío del cliente. El ubigeo (6 dígitos) lo elige el frontend de los selects de settings. */
public record DireccionRequest(
        @NotBlank(message = "Ponle un nombre a la dirección") @Size(max = 40, message = "Máximo 40 caracteres") String alias,
        @NotNull(message = Validaciones.MENSAJE_UBIGEO) @Pattern(regexp = Validaciones.UBIGEO, message = Validaciones.MENSAJE_UBIGEO) String ubigeo,
        @NotBlank(message = "Ingresa la dirección") @Size(min = 5, max = 200, message = "Debe tener entre 5 y 200 caracteres") String direccion,
        @Size(max = 200, message = "Máximo 200 caracteres") String referencia,
        @Pattern(regexp = Validaciones.CELULAR, message = Validaciones.MENSAJE_CELULAR) String telefonoContacto) {
}
