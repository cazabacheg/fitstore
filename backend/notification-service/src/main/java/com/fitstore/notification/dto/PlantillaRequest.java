package com.fitstore.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Guardar los textos y activar o desactivar una plantilla (extensión de alcance). */
public record PlantillaRequest(
        @NotBlank @Size(min = 3, max = 150) String asunto,
        @NotBlank @Size(min = 3, max = 120) String titulo,
        @NotBlank @Size(min = 3, max = 3000) String mensaje,
        @Size(max = 40) String textoBoton,
        @NotNull Boolean activa) {
}
