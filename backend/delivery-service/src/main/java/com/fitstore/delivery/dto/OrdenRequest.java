package com.fitstore.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nueva posición de una agencia en el select del checkout (la primera es 0). */
public record OrdenRequest(@NotNull @Min(0) Integer orden) {
}
