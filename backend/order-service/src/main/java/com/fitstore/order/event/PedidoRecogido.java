package com.fitstore.order.event;

import java.util.UUID;

/** {@code order.pedido.recogido}: el cliente recogió en tienda; notification confirma la entrega. */
public record PedidoRecogido(UUID pedidoId, String codigo, DestinatarioEvento destinatario) {
}
