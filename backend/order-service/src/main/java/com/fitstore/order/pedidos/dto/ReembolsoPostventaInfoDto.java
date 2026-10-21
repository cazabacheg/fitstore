package com.fitstore.order.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Si el pedido admite un reembolso postventa (extensión de alcance: siempre {@code permitido = false}). */
public record ReembolsoPostventaInfoDto(boolean permitido, BigDecimal maximo, LocalDate venceEl) {
}
