package com.fitstore.catalog.inventario.service;

import com.fitstore.common.api.ConflictoNegocioException;
import java.util.List;
import java.util.Map;

/** Alguna línea de la reserva no alcanza: 409 {@code SIN_STOCK} con los faltantes; la reserva se deshace completa. */
public class StockInsuficienteException extends ConflictoNegocioException {

    public StockInsuficienteException(List<Map<String, Object>> faltantes) {
        super("SIN_STOCK", "Sin stock para algunos productos", Map.of("faltantes", faltantes));
    }
}
