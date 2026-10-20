package com.fitstore.notification.messaging.evento;

import java.util.UUID;

/** Datos del evento {@code PedidoListoRecojo} (routing key {@code order.pedido.listo-recojo}), según el AsyncAPI. */
public record PedidoListoRecojo(UUID pedidoId, String codigo, Destinatario destinatario) {
}
