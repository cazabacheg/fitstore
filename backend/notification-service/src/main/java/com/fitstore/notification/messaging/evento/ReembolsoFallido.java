package com.fitstore.notification.messaging.evento;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos del evento {@code ReembolsoFallido} (routing key {@code payment.reembolso.fallido}), según el AsyncAPI. */
public record ReembolsoFallido(UUID pedidoId, String codigoPedido, BigDecimal monto, String ultimoError, int intentos) {
}
