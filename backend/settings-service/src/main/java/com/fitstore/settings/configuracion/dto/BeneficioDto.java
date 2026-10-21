package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Beneficio de la franja de la landing: icono (emoji o nombre corto) y texto de hasta 60 caracteres. */
public record BeneficioDto(
        @NotBlank(message = "Indica el icono del beneficio")
        @Size(max = 8, message = "El icono admite hasta 8 caracteres") String icono,
        @NotBlank(message = "Indica el texto del beneficio")
        @Size(max = 60, message = "El texto admite hasta 60 caracteres") String texto) {
}
