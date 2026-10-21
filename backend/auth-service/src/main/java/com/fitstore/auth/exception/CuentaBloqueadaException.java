package com.fitstore.auth.exception;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** 5 intentos fallidos seguidos: 423 durante el tiempo de bloqueo (CA-1-06). */
public class CuentaBloqueadaException extends FitstoreException {

    public CuentaBloqueadaException() {
        super(HttpStatus.LOCKED, "CUENTA_BLOQUEADA", "Cuenta bloqueada temporalmente");
    }
}
