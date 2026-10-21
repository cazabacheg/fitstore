package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.DireccionEnvio;
import io.swagger.v3.oas.annotations.media.Schema;

/** Dirección de entrega con los nombres del ubigeo, copiada del pedido (D3). */
public record DireccionEntregaDto(String ubigeo, String departamento, String provincia, String distrito,
        String direccion, @Schema(nullable = true) String referencia) {

    public static DireccionEntregaDto de(DireccionEnvio d) {
        return new DireccionEntregaDto(d.getUbigeo(), d.getDepartamento(), d.getProvincia(), d.getDistrito(),
                d.getDireccion(), d.getReferencia());
    }
}
