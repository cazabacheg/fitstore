package com.fitstore.order.event;

import com.fitstore.order.pedidos.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code order.pedido.creado}: order lo publica al confirmar el checkout (T1); lo consumen payment y notification. */
public record PedidoCreado(UUID pedidoId, String codigo, UUID clienteId, DestinatarioEvento destinatario,
        Modalidad modalidad, DireccionEvento direccion, List<ItemPedidoEvento> items, BigDecimal subtotal,
        BigDecimal descuentoTotal, BigDecimal costoEnvio, BigDecimal total, Instant expiraEn) {
}
