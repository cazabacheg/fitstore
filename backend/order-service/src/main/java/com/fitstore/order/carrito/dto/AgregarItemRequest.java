package com.fitstore.order.carrito.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Agregar una variante; el tope de 100 unidades por línea lo valida el servicio (CA-4-05). */
public record AgregarItemRequest(@NotNull Long varianteId, @NotNull @Min(1) Integer cantidad) {
}
