package com.fitstore.notification.messaging.evento;

import java.util.List;
import java.util.UUID;

/** Datos del evento {@code PedidoCancelado} (routing key {@code order.pedido.cancelado}), según el AsyncAPI. */
public record PedidoCancelado(UUID pedidoId, String codigo, boolean fuePagado, String motivo, String actor,
        List<ItemCantidad> items) {
}
