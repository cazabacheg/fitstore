package com.fitstore.order.carrito.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nueva cantidad de una línea; el tope de 100 unidades lo valida el servicio (CA-4-05). */
public record CambiarCantidadRequest(@NotNull @Min(1) Integer cantidad) {
}
