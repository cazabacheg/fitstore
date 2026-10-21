package com.fitstore.order.pedidos.dto;

import java.time.Instant;

/** Con qué se pagó, tal como lo guardó order desde {@code PagoConfirmado} (schema {@code PagoPedido}). */
public record PagoPedidoDto(String proveedor, String medio, Instant pagadoEn) {
}
