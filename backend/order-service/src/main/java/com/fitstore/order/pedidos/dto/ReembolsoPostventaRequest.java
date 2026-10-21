package com.fitstore.order.pedidos.dto;

import com.fitstore.order.pedidos.model.MotivoReembolso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Reembolso postventa (extensión de alcance, HU-4-12): solo declara la firma del contrato. */
public record ReembolsoPostventaRequest(@NotEmpty List<@Valid ItemReembolsoRequest> items,
        @NotNull @DecimalMin("0.01") BigDecimal monto, @NotNull MotivoReembolso motivo, @Size(max = 300) String nota) {

    public record ItemReembolsoRequest(@NotNull Long varianteId, @NotNull @jakarta.validation.constraints.Min(1) Integer cantidad) {
    }
}
