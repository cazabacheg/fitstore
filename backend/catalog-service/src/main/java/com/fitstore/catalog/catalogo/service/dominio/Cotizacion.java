package com.fitstore.catalog.catalogo.service.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Resultado del cálculo: líneas, resumen por producto y totales. El descuento no aplica al envío. */
public record Cotizacion(List<LineaCalculada> lineas, List<ResumenProducto> productos, BigDecimal subtotal,
        BigDecimal descuentoTotal, BigDecimal total) {
}
