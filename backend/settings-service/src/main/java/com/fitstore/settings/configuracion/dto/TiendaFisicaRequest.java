package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Tienda física: el distrito se elige en el selector y se guarda solo su código. */
public record TiendaFisicaRequest(
        @NotBlank(message = "Indica la dirección")
        @Size(max = 200, message = "La dirección admite hasta 200 caracteres") String direccion,
        @NotBlank(message = "Elige el distrito")
        @Pattern(regexp = "^[0-9]{6}$", message = "El distrito debe tener 6 dígitos") String ubigeo,
        @Size(max = 200, message = "La referencia admite hasta 200 caracteres") String referencia) {
}
