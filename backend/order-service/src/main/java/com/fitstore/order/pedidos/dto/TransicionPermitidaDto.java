package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;

/** Un botón de avance del backoffice (schema {@code TransicionPermitida}, O4). */
public record TransicionPermitidaDto(EstadoPedido estado, String etiqueta, boolean requiereComentario,
        boolean requiereDni) {
}
