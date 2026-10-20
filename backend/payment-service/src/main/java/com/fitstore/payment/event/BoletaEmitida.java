package com.fitstore.payment.event;

import java.util.UUID;

/** Payload de {@code payment.boleta.emitida}. {@code url} es el endpoint autenticado de descarga. Lo consumen order y notification. */
public record BoletaEmitida(UUID pedidoId, String numero, String url) {
}
