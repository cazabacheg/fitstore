package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cambiar el orden, el texto alternativo o el enlace de una imagen. */
public record ImagenRequest(
        @NotNull(message = "Indica el orden") @Min(value = 0, message = "El orden no puede ser negativo") Integer orden,
        @Size(max = 150, message = "El texto alternativo admite hasta 150 caracteres") String textoAlternativo,
        @Size(max = 300, message = "El enlace admite hasta 300 caracteres")
        @Pattern(regexp = "^(/(?!/)[^\\s]*|https://[^\\s]+)$",
                message = "Usa una ruta de la tienda (/productos) o una URL https") String enlace) {
}
