package com.fitstore.auth.service;

import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.SesionEmitida;
import java.util.UUID;

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

    /**
     * Cambia la contraseña de la propia cuenta, que debe pedir la actual. Invalida todas las sesiones y publica
     * {@code PasswordCambiado}; devuelve una sesión nueva para el dispositivo que hizo el cambio.
     *
     * @throws com.fitstore.auth.exception.ErrorCamposException si la contraseña actual es incorrecta (campo {@code passwordActual})
     */
    SesionEmitida cambiar(TipoCuenta tipo, UUID cuentaId, String passwordActual, String nuevaPassword);
}
