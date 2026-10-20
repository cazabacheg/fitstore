package com.fitstore.notification.messaging.evento;

import com.fitstore.notification.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Datos del evento {@code PedidoCreado} (routing key {@code order.pedido.creado}), según el AsyncAPI. */
public record PedidoCreado(UUID pedidoId, String codigo, UUID clienteId, Destinatario destinatario, Modalidad modalidad,
        Direccion direccion, List<ItemPedido> items, BigDecimal subtotal, BigDecimal descuentoTotal,
        BigDecimal costoEnvio, BigDecimal total, Instant expiraEn) {
}
