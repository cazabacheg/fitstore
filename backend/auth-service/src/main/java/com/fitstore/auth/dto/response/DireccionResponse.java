package com.fitstore.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Dirección del cliente. Sin {@code telefonoContacto} se usa el celular del cliente. */
public record DireccionResponse(
        @NotNull Long id,
        @NotNull String alias,
        @NotNull String ubigeo,
        @NotNull String direccion,
        @Schema(nullable = true) String referencia,
        @Schema(nullable = true) String telefonoContacto,
        boolean predeterminada) {
}
