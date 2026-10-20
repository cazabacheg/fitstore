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

public record ProductoRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "El código usa de 2 a 20 letras mayúsculas o números") String codigo,
        @NotBlank @Size(min = 3, max = 120) String nombre,
        @NotNull @Size(max = 5000) String descripcion,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal precioBase,
        List<Long> categoriaIds,
        Integer version) {
}
