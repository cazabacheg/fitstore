package com.fitstore.order.pedidos.dto;

/** Dirección de entrega con los nombres del ubigeo congelados (schema {@code DireccionPedido}). */
public record DireccionPedidoDto(String ubigeo, String departamento, String provincia, String distrito,
        String direccion, String referencia) {
}
