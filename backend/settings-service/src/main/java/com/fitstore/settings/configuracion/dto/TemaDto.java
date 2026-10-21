package com.fitstore.settings.configuracion.dto;

/** Los cinco colores del tema (#RRGGBB). */
public record TemaDto(String colorPrimario, String colorSecundario, String colorAcento, String colorFondo,
        String colorTexto) {
}
