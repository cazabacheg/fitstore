package com.fitstore.payment.dto;

/** Forma de la respuesta envuelta de un cobro (K4); documenta el 202 en el OpenAPI. */
public record RespuestaResultadoCargo(boolean success, String mensaje, ResultadoCargoDto data) {
}
