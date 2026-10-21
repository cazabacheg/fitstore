package com.fitstore.order.pedidos.dto;

import java.math.BigDecimal;

/** Línea de un pedido o de un resumen (schema {@code ItemPedido}). */
public record ItemPedidoDto(Long varianteId, Long productoId, String sku, String producto, String talla, String color,
        String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal descuentoPorcentaje,
        BigDecimal descuentoMonto, BigDecimal total) {
}
