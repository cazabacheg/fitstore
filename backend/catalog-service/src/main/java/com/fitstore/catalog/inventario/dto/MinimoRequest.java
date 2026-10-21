package com.fitstore.catalog.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record MinimoRequest(@Schema(nullable = true) @Min(0) @Max(999) Integer minimo) {
}
