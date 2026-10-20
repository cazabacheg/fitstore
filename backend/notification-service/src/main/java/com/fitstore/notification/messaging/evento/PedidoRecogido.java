package com.fitstore.notification.messaging.evento;

import java.util.UUID;

/** Datos del evento {@code PedidoRecogido} (routing key {@code order.pedido.recogido}), según el AsyncAPI. */
public record PedidoRecogido(UUID pedidoId, String codigo, Destinatario destinatario) {
}
