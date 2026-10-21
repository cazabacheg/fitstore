package com.fitstore.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Devolución a la tienda (extensión de alcance: el endpoint responde 501). */
public record DevueltaRequest(
        @NotBlank @Size(min = 3, max = 300) @Schema(example = "Cliente no ubicable tras 3 intentos") String motivo,
        @NotNull Integer version) {
}
