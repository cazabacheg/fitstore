package com.fitstore.settings.reclamaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Respuesta de la tienda al reclamo. */
public record ResponderReclamoRequest(
        @NotBlank(message = "Escribe la respuesta")
        @Size(min = 10, max = 3000, message = "La respuesta admite de 10 a 3000 caracteres") String respuesta) {
}
