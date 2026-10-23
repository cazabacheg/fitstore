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
 * (CA-2-73); así la tienda sabe qué tramos alcanza la variante sin conocer el inventario real. Ausente si el producto
 * no tiene tramos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VariantePublicaDto(Long id, String sku, Long tallaId, Long colorId, BigDecimal precio,
        Disponibilidad disponibilidad, Integer ultimasUnidades, Integer unidadesComprables) {
}
