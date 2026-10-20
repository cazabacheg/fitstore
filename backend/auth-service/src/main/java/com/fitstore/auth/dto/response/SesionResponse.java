package com.fitstore.auth.dto.response;

import com.fitstore.auth.security.ParTokens;
import com.fitstore.auth.service.SesionEmitida;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Sesión iniciada: el access token va en el cuerpo (se guarda en memoria) y el refresh en la cookie. */
public record SesionResponse(@NotNull String accessToken, @NotNull Instant expiraEn, @NotNull CuentaResponse cuenta) {

    public static SesionResponse de(SesionEmitida sesion) {
        ParTokens tokens = sesion.tokens();
        return new SesionResponse(tokens.accessToken(), tokens.accessExpiraEn(), CuentaResponse.de(sesion.cuenta()));
    }
}
