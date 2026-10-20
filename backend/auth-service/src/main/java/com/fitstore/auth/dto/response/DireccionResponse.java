package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;

/** Dirección del cliente. Sin {@code telefonoContacto} se usa el celular del cliente. */
public record DireccionResponse(
        @NotNull Long id,
        @NotNull String alias,
        @NotNull String ubigeo,
        @NotNull String direccion,
        String referencia,
        String telefonoContacto,
        boolean predeterminada) {
}
