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

public record LineaCotizadaDto(Long varianteId, Long productoId, String sku, String producto, String slug, String talla,
        String color, String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal,
        BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total, EstadoLinea estado, Integer disponible) {
}
