package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.EntregaEvento;
import com.fitstore.delivery.model.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Un paso del seguimiento: estado, quién lo registró, observación e intento. */
public record EventoSeguimientoDto(EstadoEntrega estado, @Schema(nullable = true) String observacion,
        @Schema(nullable = true, description = "null = sistema.") String actorNombre, int intento, Instant fecha) {

    public static EventoSeguimientoDto de(EntregaEvento e) {
        return new EventoSeguimientoDto(e.getEstado(), e.getObservacion(), e.getActorNombre(), e.getIntento(),
                e.getCreadoEn());
    }
}
