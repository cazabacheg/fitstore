package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.Modalidad;
import jakarta.validation.constraints.NotNull;

/** Cómo y dónde quiere recibir el cliente su compra; la dirección es obligatoria salvo en recojo. */
public record EntregaElegidaRequest(@NotNull Modalidad modalidad, Long direccionId, Long agenciaId) {
}
