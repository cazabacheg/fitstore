package com.fitstore.order.client.dto;

/** Petición de {@code POST /envios/cotizar} de delivery; {@code agenciaId} solo en la modalidad agencia. */
public record CotizarEnvioRequest(String modalidad, String ubigeo, Long agenciaId) {
}
