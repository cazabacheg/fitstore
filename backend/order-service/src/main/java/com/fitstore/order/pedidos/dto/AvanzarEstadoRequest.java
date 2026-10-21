package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Avanzar el estado desde el backoffice. {@code dniVerificado} es obligatorio y verdadero al marcar un recojo como
 * entregado (CA-4-46); {@code comentario} es el motivo obligatorio de DEVUELTO (CA-4-60).
 */
public record AvanzarEstadoRequest(@NotNull EstadoPedido nuevoEstado, @Size(max = 300) String comentario,
        Boolean dniVerificado, @NotNull Long version) {
}
