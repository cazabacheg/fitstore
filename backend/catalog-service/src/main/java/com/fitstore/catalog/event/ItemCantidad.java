package com.fitstore.catalog.event;

/** Variante y cantidad de un pedido, tal como viajan en los eventos de order. */
public record ItemCantidad(Long varianteId, int cantidad) {
}
