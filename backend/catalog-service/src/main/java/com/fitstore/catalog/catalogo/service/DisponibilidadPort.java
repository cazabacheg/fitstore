package com.fitstore.catalog.catalogo.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Puerto con el que el módulo catálogo consulta el stock sin conocer el módulo inventario (micro 10.1, ADR-019).
 * Lo implementa inventario ({@code DisponibilidadAdapter}). El stock nunca viene de la caché.
 */
public interface DisponibilidadPort {

    /** Físico y reservado de cada variante; las que no tienen fila de stock no aparecen. */
    Map<Long, Existencias> existencias(Collection<Long> varianteIds);

    /** Disponible (físico − reservado) de cada variante que tiene stock. */
    default Map<Long, Integer> disponibles(Collection<Long> varianteIds) {
        Map<Long, Integer> resultado = new HashMap<>();
        existencias(varianteIds).forEach((id, e) -> resultado.put(id, e.disponible()));
        return resultado;
    }
}
