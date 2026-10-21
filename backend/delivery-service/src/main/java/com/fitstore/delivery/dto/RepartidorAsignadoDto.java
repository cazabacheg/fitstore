package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import io.swagger.v3.oas.annotations.media.Schema;

/** Repartidor de una entrega: sin teléfono (D6). */
@Schema(nullable = true)
public record RepartidorAsignadoDto(Long id, String nombres, TipoVehiculo vehiculo,
        @Schema(nullable = true) String placa) {

    public static RepartidorAsignadoDto de(Repartidor r) {
        return r == null ? null : new RepartidorAsignadoDto(r.getId(), r.getNombres(), r.getVehiculo(), r.getPlaca());
    }
}
