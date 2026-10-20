package com.fitstore.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload de {@code payment.reembolso.fallido}. Lo consume notification (alerta al admin). */
public record ReembolsoFallido(UUID pedidoId, String codigoPedido, BigDecimal monto, String ultimoError, int intentos) {
}
