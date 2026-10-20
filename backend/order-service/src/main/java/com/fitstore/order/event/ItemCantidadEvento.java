package com.fitstore.order.event;

/** Variante y cantidad dentro de los eventos de stock (AsyncAPI {@code ItemCantidad}). */
public record ItemCantidadEvento(Long varianteId, int cantidad) {
}
