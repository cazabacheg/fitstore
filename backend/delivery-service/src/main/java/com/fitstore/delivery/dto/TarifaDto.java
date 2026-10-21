package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.model.TarifaEnvio;
import java.math.BigDecimal;

/** Tarifa de envío con su nivel; los nombres de los destinos los resuelve el frontend (P-6-05). */
public record TarifaDto(Long id, Modalidad modalidad, String ubigeo, NivelTarifa nivel, BigDecimal costo,
        int plazoDiasMin, int plazoDiasMax, boolean activa) {

    public static TarifaDto de(TarifaEnvio t) {
        return new TarifaDto(t.getId(), t.getModalidad(), t.getUbigeo(), NivelTarifa.de(t.getUbigeo()), t.getCosto(),
                t.getPlazoDiasMin(), t.getPlazoDiasMax(), t.isActiva());
    }
}
