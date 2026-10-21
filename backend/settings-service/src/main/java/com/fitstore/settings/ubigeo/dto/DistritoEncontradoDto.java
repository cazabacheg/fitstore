package com.fitstore.settings.ubigeo.dto;

/** Resultado del buscador de distritos: el distrito con su provincia y su departamento. */
public record DistritoEncontradoDto(String codigo, String nombre, String provincia, String departamento) {
}
