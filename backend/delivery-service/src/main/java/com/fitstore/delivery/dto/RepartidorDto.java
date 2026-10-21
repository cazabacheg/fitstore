package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import io.swagger.v3.oas.annotations.media.Schema;

/** Repartidor; {@code tieneEntregas} en false significa que se puede eliminar (CA-6-18). */
public record RepartidorDto(Long id, String nombres, String telefono, TipoVehiculo vehiculo,
        @Schema(nullable = true) String placa, boolean activo, boolean tieneEntregas) {

    public static RepartidorDto de(Repartidor r, boolean tieneEntregas) {
        return new RepartidorDto(r.getId(), r.getNombres(), r.getTelefono(), r.getVehiculo(), r.getPlaca(),
                r.isActivo(), tieneEntregas);
    }
}
