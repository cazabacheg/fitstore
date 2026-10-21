package com.fitstore.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Lo que payment usa de {@code GET /settings/public}: datos del emisor (boleta) y medios de pago habilitados. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfiguracionPublica(Identidad identidad, Parametros parametros) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Identidad(String nombre, String razonSocial, String ruc, String direccionFiscal, String logoClaroUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parametros(List<String> metodosPago) {
    }
}
