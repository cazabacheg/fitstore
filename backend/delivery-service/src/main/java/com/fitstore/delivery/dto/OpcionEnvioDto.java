package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Modalidad;
import java.math.BigDecimal;

/** Una modalidad disponible para un distrito, con su costo y plazo (CA-6-06). */
public record OpcionEnvioDto(Modalidad modalidad, BigDecimal costo, int plazoDiasMin, int plazoDiasMax) {
}
