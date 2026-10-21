package com.fitstore.catalog.catalogo.service;

/** Stock de una variante visto desde el catálogo: lo que hay en almacén y lo apartado por pedidos sin pagar. */
public record Existencias(int fisico, int reservado) {

    public static final Existencias VACIAS = new Existencias(0, 0);

    /** Disponible = físico − reservado (D7). */
    public int disponible() {
        return fisico - reservado;
    }
}
