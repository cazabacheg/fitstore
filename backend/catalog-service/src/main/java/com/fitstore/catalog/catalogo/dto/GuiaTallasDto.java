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

public record GuiaTallasDto(
        @NotNull @Size(min = 2) List<@NotNull String> columnas,
        @NotNull List<@NotNull List<String>> filas,
        @Schema(nullable = true) @Size(max = 300) String nota) {
}
