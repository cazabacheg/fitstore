package com.fitstore.settings.event;

import java.time.Instant;

/** Se publica al responder un reclamo ({@code settings.reclamo.respondido}). */
public record ReclamoRespondido(String numero, PersonaEvento consumidor, String respuesta, Instant respondidoEn) {
}
