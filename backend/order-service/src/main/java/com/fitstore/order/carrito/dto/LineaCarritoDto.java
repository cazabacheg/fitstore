package com.fitstore.order.carrito.dto;

import java.math.BigDecimal;

/** Línea del carrito (schema {@code LineaCarrito}); {@code estado} es OK, STOCK_INSUFICIENTE o NO_DISPONIBLE. */
public record LineaCarritoDto(Long varianteId, Long productoId, String slug, String sku, String producto,
        String talla, String color, String imagenUrl, BigDecimal precioUnitario, int cantidad,
        BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total, String estado,
        Integer disponible) {
}
