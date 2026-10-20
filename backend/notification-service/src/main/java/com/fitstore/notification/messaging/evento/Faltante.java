package com.fitstore.notification.messaging.evento;

/** Variante sin stock suficiente en {@link StockNoConfirmado}. */
public record Faltante(Long varianteId, String sku, int solicitado, int disponible) {
}
