package com.fitstore.catalog.event;

/** Evento que publica catalog cuando el disponible cruza el umbral hacia abajo ({@code catalog.stock.bajo}). */
public record StockBajo(Long varianteId, String sku, String producto, String talla, String color, int disponible, int minimo) {
}
