package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Guardar los cinco colores del tema. */
public record TemaRequest(
        @NotBlank(message = "Indica el color primario")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa el formato #RRGGBB") String colorPrimario,
        @NotBlank(message = "Indica el color secundario")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa el formato #RRGGBB") String colorSecundario,
        @NotBlank(message = "Indica el color de acento")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa el formato #RRGGBB") String colorAcento,
        @NotBlank(message = "Indica el color de fondo")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa el formato #RRGGBB") String colorFondo,
        @NotBlank(message = "Indica el color de texto")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa el formato #RRGGBB") String colorTexto,
        @NotNull(message = "Falta la versión de la configuración") Integer version) {
}
