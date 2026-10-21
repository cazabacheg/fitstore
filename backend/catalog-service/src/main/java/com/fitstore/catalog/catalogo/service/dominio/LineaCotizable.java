package com.fitstore.catalog.catalogo.service.dominio;

import java.math.BigDecimal;

/** Una línea a cotizar: la variante, a qué producto pertenece, su precio vigente y cuántas unidades se piden. */
public record LineaCotizable(Long varianteId, Long productoId, BigDecimal precioUnitario, int cantidad) {
}
