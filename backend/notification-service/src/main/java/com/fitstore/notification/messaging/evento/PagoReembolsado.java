package com.fitstore.notification.messaging.evento;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos del evento {@code PagoReembolsado} (routing key {@code payment.reembolso.completado}), según el AsyncAPI. */
public record PagoReembolsado(UUID pedidoId, String codigoPedido, UUID solicitudId, BigDecimal monto, boolean parcial,
        String motivo, boolean manual, Persona cliente) {
}
