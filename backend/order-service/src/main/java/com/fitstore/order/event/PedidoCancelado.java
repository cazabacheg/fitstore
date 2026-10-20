package com.fitstore.order.event;

import java.util.List;
import java.util.UUID;

/**
 * {@code order.pedido.cancelado}: cancelación del cliente o del staff. {@code actor} es {@code CLIENTE} o
 * {@code STAFF} (AsyncAPI); payment reembolsa si {@code fuePagado}.
 */
public record PedidoCancelado(UUID pedidoId, String codigo, boolean fuePagado, String motivo, String actor,
        List<ItemCantidadEvento> items) {
}
