package com.fitstore.notification.messaging.evento;

import java.util.List;
import java.util.UUID;

/** Datos del evento {@code PedidoPagado} (routing key {@code order.pedido.pagado}), según el AsyncAPI. */
public record PedidoPagado(UUID pedidoId, String codigo, List<ItemCantidad> items) {
}
