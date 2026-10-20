package com.fitstore.auth.event;

import com.fitstore.auth.model.MotivoToken;
import com.fitstore.auth.model.TipoCuenta;
import java.time.Instant;
import java.util.UUID;

/** Payload de {@code auth.password.reset-solicitado}. Lo consume notification (recuperación o invitación). */
public record PasswordResetSolicitado(UUID cuentaId, TipoCuenta tipoCuenta, String email, String nombres,
        MotivoToken motivo, String enlace, Instant expiraEn) {
}
