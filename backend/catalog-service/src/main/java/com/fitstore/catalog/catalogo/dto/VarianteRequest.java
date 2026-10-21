package com.fitstore.catalog.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VarianteRequest(@NotNull Long tallaId, @NotNull Long colorId, @Schema(nullable = true) @Size(max = 40) String sku,
        @Schema(nullable = true) @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal precio) {
}
