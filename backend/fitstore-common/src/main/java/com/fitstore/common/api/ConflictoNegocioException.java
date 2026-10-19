package com.fitstore.common.api;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Duplicado, conflicto de versión o regla de negocio: 409 con el código propio de cada caso (p. ej. CORREO_DUPLICADO). */
public class ConflictoNegocioException extends FitstoreException {

    public ConflictoNegocioException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public ConflictoNegocioException(String codigo, String mensaje, Map<String, Object> detalles) {
        super(HttpStatus.CONFLICT, codigo, mensaje, detalles);
    }
}
