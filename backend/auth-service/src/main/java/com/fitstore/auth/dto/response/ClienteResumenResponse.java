package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Fila del listado de clientes del backoffice. */
public record ClienteResumenResponse(
        @NotNull UUID id,
        @NotNull String email,
        @NotNull String nombres,
        @NotNull String apellidos,
        @NotNull String dni,
        @NotNull String telefono,
        boolean activo,
        @NotNull Instant creadoEn) {
}
