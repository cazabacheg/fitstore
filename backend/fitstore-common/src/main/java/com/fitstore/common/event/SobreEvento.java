package com.fitstore.common.event;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * Sobre común de todos los eventos (contracts K12): {@code {eventoId, tipo, version, ocurridoEn, datos}}.
 * Un consumidor ignora los campos que no conoce de {@code datos} (compatibilidad hacia adelante).
 */
public record SobreEvento(UUID eventoId, String tipo, int version, Instant ocurridoEn, JsonNode datos) {

    public static final int VERSION_ACTUAL = 1;
}
