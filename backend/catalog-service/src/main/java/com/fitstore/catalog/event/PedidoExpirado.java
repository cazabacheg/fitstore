package com.fitstore.catalog.event;

import java.util.UUID;

/** Evento que consume catalog de order ({@code order.pedido.expirado}): libera la reserva. */
public record PedidoExpirado(UUID pedidoId, String codigo) {
}
