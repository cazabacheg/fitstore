package com.fitstore.settings.ubigeo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Nombre de un código de 2, 4 o 6 dígitos. {@code nombre} es el del último nivel y {@code nombreCompleto}
 * va de menor a mayor ("Miraflores, Lima, Lima").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UbigeoNombreDto(String codigo, String nombre, String departamento, String provincia, String distrito,
        String nombreCompleto) {
}
