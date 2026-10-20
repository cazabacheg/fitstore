package com.fitstore.order.event;

import java.util.UUID;

/** {@code payment.pago.rechazado} (consume order): se anota en el historial; el pedido no cambia de estado. */
public record PagoRechazado(UUID pedidoId, String proveedor, String motivo) {
}
