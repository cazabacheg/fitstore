package com.fitstore.settings.configuracion.dto;

import java.time.Instant;

/** Una versión de una política; {@code tipo} va en el formato de las rutas (terminos, cambios-devoluciones…). */
public record PoliticaDto(String tipo, int version, String contenido, Instant publicadaEn) {
}
