package com.fitstore.catalog.catalogo.service.dominio;

import java.math.BigDecimal;

/** Resultado de una línea: el descuento del tramo alcanzado por su producto, redondeado half-up a 2 decimales por línea. */
public record LineaCalculada(Long varianteId, Long productoId, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal,
        BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total) {
}
