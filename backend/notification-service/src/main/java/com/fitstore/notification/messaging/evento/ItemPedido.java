package com.fitstore.notification.messaging.evento;

import java.math.BigDecimal;

/** Ítem de un pedido con su precio. */
public record ItemPedido(Long varianteId, Long productoId, String sku, String producto, String talla, String color,
        String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal descuentoPorcentaje,
        BigDecimal descuentoMonto, BigDecimal total) {
}
