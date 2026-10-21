package com.fitstore.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registrar el envío por agencia: código obligatorio, clave opcional y, si hace falta, otra agencia (CA-6-26). */
public record EnvioAgenciaRequest(
        @Schema(nullable = true, description = "Solo si se cambia la agencia elegida por el cliente.") Long agenciaId,
        @NotBlank @Size(min = 3, max = 60) String codigoSeguimiento,
        @Schema(nullable = true) @Size(max = 30) String claveRecojo,
        @NotNull Integer version) {
}
