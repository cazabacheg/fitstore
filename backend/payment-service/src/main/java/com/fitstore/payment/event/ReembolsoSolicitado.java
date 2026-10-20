package com.fitstore.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload de {@code order.reembolso.solicitado}: crea el reembolso postventa, idempotente por {@code solicitudId}. */
public record ReembolsoSolicitado(UUID pedidoId, String codigo, UUID solicitudId, BigDecimal monto,
        MotivoPostventa motivo) {
}
