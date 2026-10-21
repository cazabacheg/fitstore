package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.Modalidad;
import jakarta.validation.constraints.NotNull;

/** Cómo y dónde quiere recibir el cliente su compra; la dirección es obligatoria salvo en recojo. */
public record EntregaElegidaRequest(@NotNull Modalidad modalidad, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long direccionId, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long agenciaId) {
}
