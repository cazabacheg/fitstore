package com.fitstore.settings.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Se publica al registrar un reclamo ({@code settings.reclamo.registrado}).
 *
 * @param bien mismo formato que {@code BienContratado} de settings-service.yaml
 */
public record ReclamoRegistrado(String numero, String tipo, PersonaEvento consumidor, Map<String, Object> bien,
        String detalle, String pedidoConsumidor, LocalDate fechaLimite, Instant creadoEn) {
}
