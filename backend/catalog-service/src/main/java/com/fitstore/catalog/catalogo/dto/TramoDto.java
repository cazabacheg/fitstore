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

public record TramoDto(
        @NotNull @Min(2) Integer unidadesMinimas,
        @NotNull @DecimalMin("0") @DecimalMax("50") @Digits(integer = 3, fraction = 2) BigDecimal porcentaje) {
}
