package com.fitstore.delivery.config;

import com.fitstore.common.api.ErrorCampo;
import java.util.List;

/** Una regla de negocio de validación falló en un campo concreto: 400 {@code VALIDACION} con el campo señalado. */
public class SolicitudInvalidaException extends RuntimeException {

    private final transient List<ErrorCampo> errores;

    public SolicitudInvalidaException(String campo, String mensaje) {
        super(mensaje);
        this.errores = List.of(new ErrorCampo(campo, mensaje));
    }

    public List<ErrorCampo> errores() {
        return errores;
    }
}
