package com.fitstore.auth.service;

import com.fitstore.auth.model.TipoCuenta;

/** Recuperación de contraseña de clientes y staff (micro 4.5). */
public interface PasswordService {

    /**
     * Si la cuenta existe y está activa invalida los enlaces anteriores, genera uno de 30 min y publica
     * {@code PasswordResetSolicitado}. Nunca revela si la cuenta existe.
     */
    void solicitarRecuperacion(TipoCuenta tipo, String email);

    /**
     * Guarda la nueva contraseña de la cuenta dueña del token, marca el token como usado, cierra las demás sesiones y
     * publica {@code PasswordCambiado}.
     *
     * @throws com.fitstore.auth.exception.TokenInvalidoException si el token no existe, está usado, venció o fue reemplazado
     */
    void restablecer(String token, String nuevaPassword);
}
