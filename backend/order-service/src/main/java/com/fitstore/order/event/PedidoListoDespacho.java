package com.fitstore.order.event;

import com.fitstore.order.pedidos.model.Modalidad;
import java.util.UUID;

/** {@code order.pedido.listo-despacho}: delivery crea la entrega (delivery local o agencia). */
public record PedidoListoDespacho(UUID pedidoId, String codigo, Modalidad modalidad, DestinatarioEvento destinatario,
        DireccionEvento direccion, Long agenciaId, String agenciaNombre, String resumenItems, int bultos) {
}
