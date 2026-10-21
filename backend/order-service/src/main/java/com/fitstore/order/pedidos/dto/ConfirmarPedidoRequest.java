package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.Modalidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Confirmación del pedido; {@code totalEsperado} solo se compara con el total calculado, nunca se cobra (RF-PAY-00). */
public record ConfirmarPedidoRequest(@NotNull Modalidad modalidad, Long direccionId, Long agenciaId,
        @Size(max = 300) String notas, @NotNull @DecimalMin("0") BigDecimal totalEsperado) {

    public EntregaElegidaRequest entrega() {
        return new EntregaElegidaRequest(modalidad, direccionId, agenciaId);
    }
}
