package com.fitstore.order.event;

import java.util.List;
import java.util.UUID;

/** {@code order.pedido.pagado}: el pedido pasó a PAGADO; catalog confirma la reserva y notification avisa. */
public record PedidoPagado(UUID pedidoId, String codigo, List<ItemCantidadEvento> items) {
}
