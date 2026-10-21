package com.fitstore.settings.configuracion.dto;

import java.time.Instant;

/** Fila del historial de una política (sin el texto). */
public record VersionPoliticaDto(int version, Instant publicadaEn, String publicadaPor, boolean vigente) {
}
