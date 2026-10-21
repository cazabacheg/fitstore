package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;

/** Una fila de "Mis pedidos" (schema {@code PedidoResumen}); {@code expiraEn} solo en PENDIENTE_PAGO. */
public record PedidoResumenDto(String codigo, Instant creadoEn, EstadoPedido estado, BigDecimal total,
        Modalidad modalidad, int cantidadProductos, String miniaturaUrl, Instant expiraEn, String estadoReembolso) {
}
