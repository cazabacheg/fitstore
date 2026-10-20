package com.fitstore.catalog.event;

import java.util.List;
import java.util.UUID;

/** Evento que consume catalog de order ({@code order.pedido.cancelado}): libera la reserva o reingresa el stock. */
public record PedidoCancelado(UUID pedidoId, String codigo, boolean fuePagado, String motivo, String actor, List<ItemCantidad> items) {
}
