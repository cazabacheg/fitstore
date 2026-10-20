package com.fitstore.notification.messaging.evento;

import java.time.Instant;

/** Datos del evento {@code ReclamoRespondido} (routing key {@code settings.reclamo.respondido}), según el AsyncAPI. */
public record ReclamoRespondido(String numero, Persona consumidor, String respuesta, Instant respondidoEn) {
}
