package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record StockDto(Long varianteId, String sku, Long productoId, String producto, String talla, String color,
        int fisico, int reservado, int disponible, int minimo, boolean minimoPropio, EstadoStock estado) {
}
