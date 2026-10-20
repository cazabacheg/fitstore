package com.fitstore.order.event;

/** Dirección de entrega dentro de los eventos (AsyncAPI {@code Direccion}). */
public record DireccionEvento(String ubigeo, String departamento, String provincia, String distrito,
        String direccion, String referencia) {
}
