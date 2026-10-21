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

public record ProductoAdminDto(Long id, String codigo, String nombre, String slug, String descripcion,
        BigDecimal precioBase, EstadoProducto estado, boolean destacado, Integer ordenDestacado, ModoDescuento modoDescuento,
        List<TramoDto> tramosPropios, List<Long> categoriaIds, List<VarianteAdminDto> variantes,
        List<ImagenProductoDto> imagenes, Integer version) {
}
