package com.fitstore.order.client.dto;

import java.math.BigDecimal;

/** Costo y plazo de un envío cotizado por delivery. */
public record CotizacionEnvio(BigDecimal costo, int plazoDiasMin, int plazoDiasMax, String agenciaNombre,
        String nivelTarifa, String ubigeoTarifa) {
}
