package com.fitstore.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** No se pudo entregar: la observación es obligatoria (CA-6-22). */
public record ObservacionRequest(@NotBlank @Size(min = 3, max = 300) String observacion, @NotNull Integer version) {
}
