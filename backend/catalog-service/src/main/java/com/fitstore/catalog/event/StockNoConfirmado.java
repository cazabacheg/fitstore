package com.fitstore.catalog.event;

import java.util.List;
import java.util.UUID;

/**
 * Evento que publica catalog cuando un pago llega con la reserva vencida y no hay stock
 * ({@code catalog.stock.no-confirmado}). Según el AsyncAPI: cada faltante trae {@code varianteId}, {@code sku},
 * {@code solicitado} y {@code disponible}.
 */
public record StockNoConfirmado(UUID pedidoId, List<Faltante> faltantes) {

    public record Faltante(Long varianteId, String sku, int solicitado, int disponible) {
    }
}
