package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Una fila de la tabla de pedidos del backoffice (schema {@code PedidoAdminResumen}). */
public record PedidoAdminResumenDto(UUID id, String codigo, Instant creadoEn, String clienteNombre,
        Modalidad modalidad, String agenciaNombre, EstadoPedido estado, BigDecimal total, String estadoReembolso) {
}
