package com.fitstore.auth.exception;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** Correo inexistente, contraseña errada, cuenta inactiva o invitación pendiente: siempre el mismo mensaje (CA-1-05). */
public class CredencialesInvalidasException extends FitstoreException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }
}
