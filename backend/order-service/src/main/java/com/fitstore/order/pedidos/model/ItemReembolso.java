package com.fitstore.order.pedidos.model;

/** Línea del reembolso postventa: lo que se devuelve, guardado en la columna jsonb pedido_reembolso.items. */
public record ItemReembolso(Long varianteId, String sku, int cantidad) {
}
