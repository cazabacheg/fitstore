package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Una parada de la hoja de ruta (extensión de alcance). */
public record ItemHojaRutaDto(UUID id, String codigoPedido, EstadoEntrega estado, String destinatarioNombre,
        String telefono, String direccion, String distrito, @Schema(nullable = true) String referencia, int bultos,
        int intento) {
}
