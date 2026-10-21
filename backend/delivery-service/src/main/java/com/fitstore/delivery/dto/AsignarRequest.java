package com.fitstore.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Asignar o reprogramar: repartidor activo y fecha de hoy o posterior (E7). */
public record AsignarRequest(
        @NotNull Long repartidorId,
        @NotNull @Schema(description = "Hoy o posterior (E7).") LocalDate fechaProgramada,
        @NotNull Integer version) {
}
