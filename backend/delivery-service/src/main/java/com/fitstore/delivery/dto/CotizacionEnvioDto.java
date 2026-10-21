package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.NivelTarifa;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Costo y plazo de un envío, con el nivel de la tarifa aplicada (E3). */
public record CotizacionEnvioDto(
        BigDecimal costo,
        int plazoDiasMin,
        int plazoDiasMax,
        @Schema(nullable = true) String agenciaNombre,
        NivelTarifa nivelTarifa,
        String ubigeoTarifa) {
}
