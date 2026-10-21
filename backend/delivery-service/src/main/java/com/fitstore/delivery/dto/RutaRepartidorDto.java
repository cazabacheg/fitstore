package com.fitstore.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Las entregas de un repartidor en el día, ordenadas por distrito (extensión de alcance). */
public record RutaRepartidorDto(RepartidorAsignadoDto repartidor,
        @Schema(description = "Ordenadas por distrito (CA-6-29).") List<ItemHojaRutaDto> entregas) {
}
