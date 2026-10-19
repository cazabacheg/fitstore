package com.fitstore.auth.event;

import com.fitstore.auth.model.TipoCuenta;
import java.util.UUID;

/** Payload de {@code auth.password.cambiado}. Lo consume notification (aviso de seguridad). */
public record PasswordCambiado(UUID cuentaId, TipoCuenta tipoCuenta, String email, String nombres) {
}
