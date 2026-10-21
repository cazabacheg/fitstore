package com.fitstore.settings.reclamaciones.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Lo que ve el consumidor al registrar: su número y la fecha límite de respuesta. */
public record ReclamoRegistradoDto(String numero, LocalDate fechaLimite, Instant creadoEn) {
}
