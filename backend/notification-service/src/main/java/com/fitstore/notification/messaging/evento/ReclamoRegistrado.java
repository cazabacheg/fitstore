package com.fitstore.notification.messaging.evento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Datos del evento {@code ReclamoRegistrado} (routing key {@code settings.reclamo.registrado}), según el AsyncAPI. */
public record ReclamoRegistrado(String numero, String tipo, Persona consumidor, Map<String, Object> bien, String detalle,
        String pedidoConsumidor, LocalDate fechaLimite, Instant creadoEn) {
}
