package com.fitstore.catalog.catalogo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Órdenes del listado público (micro 4.3): {@code novedad} (por defecto), {@code precio,asc}, {@code precio,desc}, {@code nombre}. */
public final class OrdenProductos {

    public static final String NOVEDAD = "novedad";

    private OrdenProductos() {
    }

    public static Pageable paginacion(int pagina, int tamano, String orden) {
        Sort sort = switch (orden == null ? NOVEDAD : orden) {
            case "precio,asc" -> Sort.by(Sort.Order.asc("precioDesde"), Sort.Order.desc("id"));
            case "precio,desc" -> Sort.by(Sort.Order.desc("precioDesde"), Sort.Order.desc("id"));
            case "nombre" -> Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id"));
        };
        return PageRequest.of(pagina, tamano, sort);
    }
}
