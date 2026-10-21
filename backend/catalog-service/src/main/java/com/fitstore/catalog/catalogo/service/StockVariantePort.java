package com.fitstore.catalog.catalogo.service;

import java.util.Collection;
import java.util.UUID;

/**
 * Operaciones de inventario que necesita el módulo catálogo al crear o borrar variantes (micro 10.1). Lo implementa
 * inventario ({@code DisponibilidadAdapter}); catálogo nunca toca las tablas de stock.
 */
public interface StockVariantePort {

    /** Crea la fila de stock de una variante nueva con {@code fisico = 0}. */
    void crearStock(Long varianteId);

    /**
     * Crea la fila de stock con una entrada inicial: deja el movimiento {@code ENTRADA} con el motivo y el usuario
     * (carga masiva, CA-2-60). Si {@code cantidad} es 0 solo crea la fila.
     */
    void crearStockConEntrada(Long varianteId, int cantidad, String motivo, UUID usuarioId);

    /** {@code true} si alguna de las variantes tiene movimientos o reservas (no se pueden borrar, CA-2-26). */
    boolean tieneMovimientosOReservas(Collection<Long> varianteIds);

    /** Borra la fila de stock de variantes sin movimientos ni reservas. */
    void eliminarStock(Collection<Long> varianteIds);
}
