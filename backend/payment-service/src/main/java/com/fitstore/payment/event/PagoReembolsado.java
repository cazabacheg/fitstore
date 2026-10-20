package com.fitstore.payment.event;

import com.fitstore.payment.model.MotivoReembolso;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de {@code payment.reembolso.completado}. {@code solicitudId} solo viene en reembolsos postventa.
 * Lo consumen notification (avisa al cliente) y order (registra lo reembolsado).
 */
public record PagoReembolsado(UUID pedidoId, String codigoPedido, UUID solicitudId, BigDecimal monto, boolean parcial,
        MotivoReembolso motivo, boolean manual, Persona cliente) {
}
