package com.fitstore.order.client.dto;

import java.util.List;

/** Petición de {@code POST /api/v1/precios/cotizar} de catalog (máximo 50 líneas). */
public record CotizarCatalogRequest(List<Linea> lineas) {

    public record Linea(long varianteId, int cantidad) {
    }
}
