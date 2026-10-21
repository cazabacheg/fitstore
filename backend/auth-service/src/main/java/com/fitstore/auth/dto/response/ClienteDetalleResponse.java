package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cliente con sus direcciones, para el detalle del backoffice (desde aquí se enlaza a {@code /pedidos?clienteId=…}). */
public record ClienteDetalleResponse(
        @NotNull UUID id,
        @NotNull String email,
        @NotNull String nombres,
        @NotNull String apellidos,
        @NotNull String dni,
        @NotNull String telefono,
        boolean activo,
        @NotNull Instant creadoEn,
        Instant ultimoLoginEn,
        @NotNull List<DireccionResponse> direcciones) {
}
