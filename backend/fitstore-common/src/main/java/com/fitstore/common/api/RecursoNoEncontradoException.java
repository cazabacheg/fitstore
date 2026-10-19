package com.fitstore.common.api;

import org.springframework.http.HttpStatus;

/** El recurso no existe o no pertenece al usuario: 404 {@code NO_ENCONTRADO}. */
public class RecursoNoEncontradoException extends FitstoreException {

    public static final String CODIGO = "NO_ENCONTRADO";

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, CODIGO, mensaje);
    }
}
