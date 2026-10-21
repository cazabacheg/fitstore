package com.fitstore.order.pedidos.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Respuesta de la confirmación (schema {@code PedidoCreado}). */
public record PedidoCreadoDto(String codigo, BigDecimal total, Instant expiraEn) {
}
