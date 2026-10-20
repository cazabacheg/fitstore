package com.fitstore.order.event;

import java.util.UUID;

/** {@code order.pedido.listo-recojo}: notification avisa "ya puedes recoger tu pedido". */
public record PedidoListoRecojo(UUID pedidoId, String codigo, DestinatarioEvento destinatario) {
}
