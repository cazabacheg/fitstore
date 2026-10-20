package com.fitstore.catalog.catalogo.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros del listado público (micro 4.3).
 *
 * @param q               texto a buscar en nombre y descripción, sin tildes ni mayúsculas
 * @param categoria       slug de la categoría; incluye a sus hijas
 * @param tallas          códigos de talla; el producto aparece si alguna variante coincide
 * @param colores         ids de color
 * @param precioMin       sobre {@code precio_desde}
 * @param precioMax       sobre {@code precio_desde}
 * @param soloDisponibles solo productos con stock disponible
 */
public record FiltroProductos(String q, String categoria, List<String> tallas, List<Long> colores, BigDecimal precioMin,
        BigDecimal precioMax, boolean soloDisponibles) {

    public FiltroProductos {
        tallas = tallas == null ? List.of() : tallas;
        colores = colores == null ? List.of() : colores;
    }
}
