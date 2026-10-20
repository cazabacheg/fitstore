package com.fitstore.auth.security;

import com.fitstore.auth.model.Cuenta;

/** Resultado de iniciar una sesión: la cuenta y los tokens emitidos. */
public record SesionEmitida(Cuenta cuenta, ParTokens tokens) {
}
