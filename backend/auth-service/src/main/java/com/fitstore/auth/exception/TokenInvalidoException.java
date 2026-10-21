package com.fitstore.auth.exception;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** Enlace de recuperación o invitación inexistente, usado, vencido o reemplazado (CA-1-14). */
public class TokenInvalidoException extends FitstoreException {

    public TokenInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Enlace inválido o expirado");
    }
}
