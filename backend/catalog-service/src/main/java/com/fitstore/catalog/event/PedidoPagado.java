package com.fitstore.catalog.event;

import java.util.List;
import java.util.UUID;

/** Evento que consume catalog de order ({@code order.pedido.pagado}): confirma la reserva. */
public record PedidoPagado(UUID pedidoId, String codigo, List<ItemCantidad> items) {
}
