package com.fitstore.auth.service;

import com.fitstore.auth.model.Cuenta;
import com.fitstore.auth.security.ParTokens;

/** Resultado de iniciar una sesión: la cuenta y los tokens emitidos. */
public record SesionEmitida(Cuenta cuenta, ParTokens tokens) {
}
