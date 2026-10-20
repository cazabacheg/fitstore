package com.fitstore.notification.messaging.evento;

import java.util.List;
import java.util.UUID;

/** Datos del evento {@code StockNoConfirmado} (routing key {@code catalog.stock.no-confirmado}), según el AsyncAPI. */
public record StockNoConfirmado(UUID pedidoId, List<Faltante> faltantes) {
}
