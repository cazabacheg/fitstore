package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Perfil del cliente (también lo lee order-service en el checkout). El DNI solo lo ve el dueño y el staff. */
public record ClienteResponse(
        @NotNull UUID id,
        @NotNull String email,
        @NotNull String nombres,
        @NotNull String apellidos,
        @NotNull String dni,
        @NotNull String telefono,
        @NotNull Instant creadoEn) {
}
