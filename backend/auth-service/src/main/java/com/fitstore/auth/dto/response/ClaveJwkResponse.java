package com.fitstore.auth.dto.response;

import jakarta.validation.constraints.NotNull;

/** Clave pública RSA en formato JWK (RFC 7517). */
public record ClaveJwkResponse(@NotNull String kty, @NotNull String kid, @NotNull String use, @NotNull String alg,
        @NotNull String n, @NotNull String e) {
}
