package com.fitstore.catalog.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fitstore.catalog.inventario.model.TipoMovimiento;

public record MovimientoDto(Long id, TipoMovimiento tipo, int cantidad, int saldoFisico, String motivo, String referencia,
        String usuarioNombre, Instant creadoEn) {
}
