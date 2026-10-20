package com.fitstore.catalog.catalogo.service.carga;

import com.fitstore.catalog.catalogo.dto.ErrorFilaDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lo que la validación de la carga masiva deja guardado 30 minutos para poder confirmarla (micro 4.4.1). */
public final class ModeloCarga {

    private ModeloCarga() {
    }

    public record VarianteCarga(Long tallaId, Long colorId, BigDecimal precio, int stockInicial) {
    }

    public record ProductoCarga(String codigo, String nombre, String descripcion, BigDecimal precioBase,
            List<Long> categoriaIds, List<VarianteCarga> variantes) {
    }

    public record ValidacionGuardada(UUID id, Instant expiraEn, int filas, List<ErrorFilaDto> errores,
            List<ProductoCarga> productos) {

        public boolean valida() {
            return errores.isEmpty();
        }
    }
}
