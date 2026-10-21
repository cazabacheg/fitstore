package com.fitstore.order.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cancelación desde el backoffice: motivo obligatorio (hasta 300 caracteres) y la versión que vio el operador. */
public record CancelarStaffRequest(@NotBlank @Size(min = 3, max = 300) String motivo, @NotNull Integer version) {
}
