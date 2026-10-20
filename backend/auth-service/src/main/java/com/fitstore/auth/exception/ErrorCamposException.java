package com.fitstore.auth.exception;

import com.fitstore.common.api.ErrorCampo;
import com.fitstore.common.api.FitstoreException;
import java.util.List;
import org.springframework.http.HttpStatus;

/** 400 de validación decidida por el servicio (p. ej. contraseña actual incorrecta), con el campo que falló. */
public class ErrorCamposException extends FitstoreException {

    private final transient List<ErrorCampo> errores;

    public ErrorCamposException(String campo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, "VALIDACION", "Revisa los datos ingresados");
        this.errores = List.of(new ErrorCampo(campo, mensaje));
    }

    public List<ErrorCampo> errores() {
        return errores;
    }
}
