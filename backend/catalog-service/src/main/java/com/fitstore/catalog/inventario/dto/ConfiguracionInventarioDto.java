package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record ConfiguracionInventarioDto(
        @NotNull @Min(0) @Max(999) Integer stockMinimoPorDefecto,
        @NotNull @Min(0) @Max(30) Integer margenReservaMinutos) {
}
