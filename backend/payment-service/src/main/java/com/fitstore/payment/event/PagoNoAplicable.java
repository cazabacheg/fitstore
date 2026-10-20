package com.fitstore.payment.event;

import java.util.UUID;

/** Payload de {@code order.pago.no-aplicable}: el pago llegó tarde y payment crea el reembolso total. {@code pagoId} es la intención. */
public record PagoNoAplicable(UUID pedidoId, UUID pagoId, MotivoNoAplicable motivo) {
}
