package com.fitstore.common.api;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Base de las excepciones de negocio: llevan el estado HTTP y un {@code codigo} estable para el frontend (K5). */
public class FitstoreException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;
    private final transient Map<String, Object> detalles;

    public FitstoreException(HttpStatus estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, null);
    }

    public FitstoreException(HttpStatus estado, String codigo, String mensaje, Map<String, Object> detalles) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }

    public Map<String, Object> detalles() {
        return detalles;
    }
}
