package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record ReservaRequest(@NotNull UUID pedidoId, @NotNull Instant expiraEnPedido,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid ItemReservaDto> items) {
}
