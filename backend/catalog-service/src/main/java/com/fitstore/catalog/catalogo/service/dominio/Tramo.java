package com.fitstore.catalog.catalogo.service.dominio;

import java.math.BigDecimal;

/** Un tramo de descuento por volumen: desde {@code unidadesMinimas} unidades se aplica {@code porcentaje} %. */
public record Tramo(int unidadesMinimas, BigDecimal porcentaje) {
}
