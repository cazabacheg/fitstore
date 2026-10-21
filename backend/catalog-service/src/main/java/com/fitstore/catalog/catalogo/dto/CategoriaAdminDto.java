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

public record CategoriaAdminDto(Long id, Long padreId, String nombre, String slug, int orden, boolean activo,
        int cantidadProductos, boolean tieneGuiaTallas, List<CategoriaAdminHijaDto> hijas) {
}
