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

/**
 * {@code unidadesComprables}: stock disponible acotado a las unidades del tramo de descuento más alto del producto
 * (CA-2-90); ausente si el producto no tiene tramos o la línea es NO_DISPONIBLE.
 */
public record LineaCotizadaDto(Long varianteId, Long productoId, String sku, String producto, String slug, String talla,
        String color, String imagenUrl, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal,
        BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto, BigDecimal total, EstadoLinea estado, Integer disponible,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer unidadesComprables) {
}
