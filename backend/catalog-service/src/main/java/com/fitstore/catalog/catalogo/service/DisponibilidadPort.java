package com.fitstore.catalog.catalogo.service;

import java.util.Collection;
import java.util.Map;

/**
 * Puerto con el que el módulo catálogo consulta el stock sin conocer el módulo inventario (micro 10.1, ADR-019).
 * Lo implementa inventario ({@code DisponibilidadAdapter}).
 */
public interface DisponibilidadPort {

    /** Disponible (físico − reservado) de cada variante; las que no tienen stock devuelven 0. Nunca viene de la caché. */
    Map<Long, Integer> disponibles(Collection<Long> varianteIds);
}
