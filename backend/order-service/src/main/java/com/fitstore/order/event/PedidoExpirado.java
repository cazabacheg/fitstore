package com.fitstore.order.event;

import java.util.UUID;

/** {@code order.pedido.expirado}: venció el plazo de pago; catalog libera el stock y payment anula la intención. */
public record PedidoExpirado(UUID pedidoId, String codigo) {
}
