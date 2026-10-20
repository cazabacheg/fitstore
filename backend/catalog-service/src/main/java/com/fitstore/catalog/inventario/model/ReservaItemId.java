package com.fitstore.catalog.inventario.model;

import java.io.Serializable;
import java.util.UUID;

/** Clave compuesta de {@link ReservaItem}. */
public record ReservaItemId(UUID pedidoId, Long varianteId) implements Serializable {
}
