package com.fitstore.payment.event;

import java.util.List;
import java.util.UUID;

/**
 * Payload de {@code order.pedido.cancelado}. Con {@code fuePagado = true} payment crea el reembolso total; si no,
 * anula la intención.
 */
public record PedidoCancelado(UUID pedidoId, String codigo, boolean fuePagado, String motivo, ActorCancelacion actor,
        List<ItemCantidad> items) {

    /** Variante y cantidad de un ítem (esquema {@code ItemCantidad}). */
    public record ItemCantidad(Long varianteId, int cantidad) {
    }
}
