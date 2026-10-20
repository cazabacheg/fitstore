package com.fitstore.notification.messaging.evento;

/** Datos del evento {@code StockBajo} (routing key {@code catalog.stock.bajo}), según el AsyncAPI. */
public record StockBajo(Long varianteId, String sku, String producto, String talla, String color, int disponible,
        int minimo) {
}
