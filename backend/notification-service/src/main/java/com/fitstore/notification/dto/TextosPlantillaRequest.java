package com.fitstore.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Textos del editor para la vista previa y la prueba (extensión de alcance). */
public record TextosPlantillaRequest(
        @NotBlank @Size(min = 3, max = 150) String asunto,
        @NotBlank @Size(min = 3, max = 120) String titulo,
        @NotBlank @Size(min = 3, max = 3000) String mensaje,
        @Size(max = 40) String textoBoton) {
}
