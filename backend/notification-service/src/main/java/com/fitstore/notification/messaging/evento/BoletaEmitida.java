package com.fitstore.notification.messaging.evento;

import java.util.UUID;

/** Datos del evento {@code BoletaEmitida} (routing key {@code payment.boleta.emitida}), según el AsyncAPI. */
public record BoletaEmitida(UUID pedidoId, String numero, String url) {
}
