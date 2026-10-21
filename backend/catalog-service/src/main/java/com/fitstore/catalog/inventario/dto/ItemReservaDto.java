package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record ItemReservaDto(@NotNull Long varianteId, @NotNull @Min(1) @Max(100) Integer cantidad) {
}
