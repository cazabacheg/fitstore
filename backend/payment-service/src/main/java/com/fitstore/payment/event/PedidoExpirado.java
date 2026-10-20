package com.fitstore.payment.event;

import java.util.UUID;

/** Payload de {@code order.pedido.expirado}: payment anula la intención. */
public record PedidoExpirado(UUID pedidoId, String codigo) {
}
