package com.fitstore.order.event;

import java.math.BigDecimal;

/** Línea del pedido dentro de {@code PedidoCreado} (AsyncAPI {@code ItemPedido}). */
public record ItemPedidoEvento(Long varianteId, Long productoId, String sku, String producto, String talla,
        String color, String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal descuentoPorcentaje,
        BigDecimal descuentoMonto, BigDecimal total) {
}
