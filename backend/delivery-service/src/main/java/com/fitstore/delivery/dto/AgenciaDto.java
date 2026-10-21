package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Agencia;
import io.swagger.v3.oas.annotations.media.Schema;

/** Agencia de transporte; {@code urlSeguimiento} es una plantilla con {@code {codigo}} (D4). */
public record AgenciaDto(Long id, String nombre, @Schema(nullable = true) String urlSeguimiento, boolean activa,
        int orden) {

    public static AgenciaDto de(Agencia a) {
        return new AgenciaDto(a.getId(), a.getNombre(), a.getUrlSeguimiento(), a.isActiva(), a.getOrden());
    }
}
