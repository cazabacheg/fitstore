package com.fitstore.order.carrito.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Línea del carrito (schema {@code LineaCarrito}); {@code estado} es OK, STOCK_INSUFICIENTE o NO_DISPONIBLE.
 * {@code unidadesComprables} es el stock acotado al tramo de descuento más alto, tal como lo cotiza catalog (CA-4-80).
 */
public record LineaCarritoDto(Long varianteId, Long productoId, String slug, String sku, String producto,
        String talla, String color, String imagenUrl, BigDecimal precioUnitario, int cantidad,
        BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total, String estado,
        Integer disponible, @JsonInclude(JsonInclude.Include.NON_NULL) Integer unidadesComprables) {
}
