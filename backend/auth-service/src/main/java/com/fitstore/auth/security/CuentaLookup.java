package com.fitstore.auth.security;

import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.model.TipoCuenta;
import java.util.Optional;
import java.util.UUID;

/** Decide en qué tabla (cliente o staff) se busca una cuenta según su {@link TipoCuenta} (D2). */
public interface CuentaLookup {

    Optional<Cuenta> buscar(TipoCuenta tipo, UUID id);

    /** Busca por correo ya normalizado. */
    Optional<Cuenta> buscarPorEmail(TipoCuenta tipo, String email);
}
