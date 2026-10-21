package com.fitstore.auth.exception;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** Refresh token inválido, vencido, revocado, reutilizado o anterior a un cambio de credenciales: 401. */
public class SesionInvalidaException extends FitstoreException {

    public SesionInvalidaException() {
        super(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Tu sesión venció, vuelve a ingresar");
    }
}
