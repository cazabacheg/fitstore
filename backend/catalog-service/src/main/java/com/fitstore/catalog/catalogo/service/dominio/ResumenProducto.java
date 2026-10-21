package com.fitstore.catalog.catalogo.service.dominio;

/**
 * Resumen por producto: las unidades sumadas de todas sus líneas, el tramo alcanzado (o {@code null}) y el siguiente
 * con cuántas unidades faltan para alcanzarlo (o {@code null} si ya está en el último).
 */
public record ResumenProducto(Long productoId, int unidades, Tramo tramoAlcanzado, Tramo siguienteTramo, int faltan) {
}
