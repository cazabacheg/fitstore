package com.fitstore.settings.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Guardar la identidad: el RUC lleva 11 dígitos y {@code version} evita pisar el cambio de otro admin. */
public record IdentidadRequest(
        @NotBlank(message = "Indica el nombre de la tienda")
        @Size(min = 2, max = 60, message = "El nombre admite de 2 a 60 caracteres") String nombre,
        @Size(max = 120, message = "El eslogan admite hasta 120 caracteres") String eslogan,
        @NotBlank(message = "Indica la razón social")
        @Size(min = 2, max = 150, message = "La razón social admite de 2 a 150 caracteres") String razonSocial,
        @NotBlank(message = "Indica el RUC")
        @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 dígitos") String ruc,
        @NotBlank(message = "Indica la dirección fiscal")
        @Size(min = 5, max = 200, message = "La dirección fiscal admite de 5 a 200 caracteres") String direccionFiscal,
        @NotNull(message = "Falta la versión de la configuración") Integer version) {
}
