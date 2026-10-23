package com.fitstore.order.client.dto;

import java.math.BigDecimal;
import java.util.List;

/** Respuesta de {@code POST /api/v1/precios/cotizar} de catalog (schema {@code Cotizacion}). */
public record CotizacionCatalog(List<LineaCotizada> lineas, List<ProductoCotizado> productos, BigDecimal subtotal,
        BigDecimal descuentoTotal, BigDecimal total) {

    /** Estado de una línea cotizada: OK, STOCK_INSUFICIENTE o NO_DISPONIBLE. */
    public static final String OK = "OK";
    public static final String STOCK_INSUFICIENTE = "STOCK_INSUFICIENTE";
    public static final String NO_DISPONIBLE = "NO_DISPONIBLE";

    public record LineaCotizada(Long varianteId, Long productoId, String sku, String producto, String slug,
            String talla, String color, String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal,
            BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total, String estado,
            Integer disponible, Integer unidadesComprables) {
    }

    public record ProductoCotizado(Long productoId, String nombre, int unidades, Tramo tramoAlcanzado,
            SiguienteTramo siguienteTramo) {
    }

    public record Tramo(int unidadesMinimas, BigDecimal porcentaje) {
    }

    public record SiguienteTramo(int unidadesMinimas, BigDecimal porcentaje, int faltan) {
    }
}
