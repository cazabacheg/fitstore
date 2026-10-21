package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.TipoVehiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta o edición de un repartidor. La placa es obligatoria para moto y auto (lo valida el servicio). */
public record RepartidorRequest(
        @NotBlank @Size(min = 3, max = 120) String nombres,
        @NotNull @Pattern(regexp = "^9[0-9]{8}$", message = "Ingresa un celular de 9 dígitos que empiece con 9")
        @Schema(description = "Celular peruano de 9 dígitos que empieza con 9.") String telefono,
        @NotNull TipoVehiculo vehiculo,
        @Schema(nullable = true, description = "Obligatoria en MOTO y AUTO.")
        @Pattern(regexp = "^[A-Z0-9]{3}-?[A-Z0-9]{3}$", message = "Ingresa una placa válida, por ejemplo ABC-123")
        String placa) {
}
