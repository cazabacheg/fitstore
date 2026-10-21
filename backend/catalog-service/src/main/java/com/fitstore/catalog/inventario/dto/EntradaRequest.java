package com.fitstore.catalog.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record EntradaRequest(@NotEmpty @Size(max = 100) List<@NotNull @Valid LineaEntradaRequest> lineas,
        @Schema(nullable = true) @Size(max = 200) String motivo) {
}
