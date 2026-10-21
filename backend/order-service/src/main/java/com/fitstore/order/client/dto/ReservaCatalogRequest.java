package com.fitstore.order.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Petición de {@code POST /internal/v1/inventario/reservas} de catalog. */
public record ReservaCatalogRequest(UUID pedidoId, Instant expiraEnPedido, List<Item> items) {

    public record Item(long varianteId, int cantidad) {
    }
}
