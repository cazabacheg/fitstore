package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Publicar una nueva versión de una política, en Markdown. */
public record PublicarPoliticaRequest(
        @NotBlank(message = "Escribe el texto de la política")
        @Size(min = 50, max = 50000, message = "El texto debe tener entre 50 y 50 000 caracteres") String contenido) {
}
