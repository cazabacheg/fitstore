package com.fitstore.catalog.catalogo.dto;

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
        @NotNull @Size(min = 2) List<@NotBlank String> columnas,
        @NotNull List<@NotNull List<String>> filas,
        @Size(max = 300) String nota) {
}
