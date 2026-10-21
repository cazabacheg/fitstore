package com.fitstore.auth.service;

import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.SesionEmitida;

/** Login de clientes y staff, refresh y logout (micro 4.3 y 4.4). */
public interface AutenticacionService {

    /**
     * Inicia sesión. Cualquier fallo (correo inexistente, contraseña errada, cuenta inactiva, invitación pendiente)
     * responde igual; 5 fallos seguidos bloquean el correo 15 min, por separado para clientes y staff.
     */
    SesionEmitida iniciarSesion(TipoCuenta tipo, String email, String password);

    /** Nuevo par de tokens a partir de la cookie de refresh, que se rota. */
    SesionEmitida refrescar(String refreshToken);

    /** Revoca el refresh token. No falla si ya no es válido. */
    void cerrarSesion(String refreshToken);
}
