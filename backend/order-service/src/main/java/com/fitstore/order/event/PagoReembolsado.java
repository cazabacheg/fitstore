package com.fitstore.order.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code payment.reembolso.completado} (consume order): si trae {@code solicitudId} actualiza
 * {@code monto_reembolsado} del pedido.
 */
public record PagoReembolsado(UUID pedidoId, String codigoPedido, UUID solicitudId, BigDecimal monto,
        boolean parcial, String motivo, boolean manual, PersonaEvento cliente) {
}
