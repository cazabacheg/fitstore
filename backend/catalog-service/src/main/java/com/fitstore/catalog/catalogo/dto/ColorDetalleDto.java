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

/** Color de un producto; {@code conStock} es falso si ninguna talla tiene stock (CA-2-09). */
public record ColorDetalleDto(Long id, String nombre, String hex, boolean activo, boolean conStock) {
}
