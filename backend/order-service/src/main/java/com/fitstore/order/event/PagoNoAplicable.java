package com.fitstore.order.event;

import java.util.UUID;

/**
 * {@code order.pago.no-aplicable}: llegó un pago con el pedido ya expirado o cancelado; payment reembolsa. El
 * {@code motivo} es {@code PEDIDO_EXPIRADO} o {@code PEDIDO_CANCELADO} (AsyncAPI).
 */
public record PagoNoAplicable(UUID pedidoId, UUID pagoId, String motivo) {
}
