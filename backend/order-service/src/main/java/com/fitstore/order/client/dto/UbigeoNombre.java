package com.fitstore.order.client.dto;

/** Nombre de un ubigeo de settings ({@code GET /ubigeo/{codigo}}). */
public record UbigeoNombre(String codigo, String nombre, String departamento, String provincia, String distrito,
        String nombreCompleto) {
}
