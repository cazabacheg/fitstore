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

public record ColorRequest(@NotBlank @Size(min = 2, max = 40) String nombre,
        @NotNull @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB") String hex,
        Boolean activo) {
}
