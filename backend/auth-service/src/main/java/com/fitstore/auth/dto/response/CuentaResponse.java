package com.fitstore.auth.dto.response;

import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.TipoCuenta;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Datos de la cuenta que inició sesión: los mismos del JWT, sin el DNI. */
public record CuentaResponse(
        @NotNull UUID id,
        @NotNull TipoCuenta tipo,
        @NotNull String rol,
        @NotNull String email,
        @NotNull String nombres,
        @NotNull String apellidos) {

    public static CuentaResponse de(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.tipo(), cuenta.rol(), cuenta.getEmail(),
                cuenta.getNombres(), cuenta.getApellidos());
    }
}
