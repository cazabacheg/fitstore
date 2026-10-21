package com.fitstore.payment.dto;

import java.time.Instant;

/** Número y fecha de la boleta de un pago; el detalle la muestra como nula mientras no se emite. */
public record BoletaInfo(String numero, Instant emitidaEn) {
}
