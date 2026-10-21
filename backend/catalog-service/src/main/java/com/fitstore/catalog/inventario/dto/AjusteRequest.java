package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record AjusteRequest(@NotNull Long varianteId, @NotNull Integer cantidad,
        @NotBlank @Size(min = 3, max = 200) String motivo) {
}
