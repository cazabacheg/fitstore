package com.fitstore.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta o edición de una agencia. La URL de seguimiento es una plantilla https con {@code {codigo}} (D4). */
public record AgenciaRequest(
        @NotBlank @Size(min = 2, max = 60) String nombre,
        @Schema(nullable = true) @Size(max = 300)
        @Pattern(regexp = "^https://.*\\{codigo\\}.*$", message = "La URL debe ser https e incluir {codigo}")
        String urlSeguimiento) {
}
