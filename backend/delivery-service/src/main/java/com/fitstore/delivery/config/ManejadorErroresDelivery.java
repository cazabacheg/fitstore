package com.fitstore.delivery.config;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores de delivery: los comunes de {@code fitstore-common} más validación por campo y conflicto de versión. */
@RestControllerAdvice
public class ManejadorErroresDelivery extends GlobalExceptionHandlerBase {

    public static final String MENSAJE_VERSION = "La entrega cambió; recarga la página";

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> alSerInvalida(SolicitudInvalidaException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.de(e.getMessage(), "VALIDACION").conErrores(e.errores()), peticion);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> alPisarseLaVersion(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT, ApiError.de(MENSAJE_VERSION, "CONFLICTO_VERSION"), peticion);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> alViolarUnaRestriccion(DataIntegrityViolationException e,
            HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT,
                ApiError.de("El dato ya fue modificado por otra persona; vuelve a intentarlo", "CONFLICTO"), peticion);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> alUsarUnMetodoNoPermitido(HttpRequestMethodNotSupportedException e,
            HttpServletRequest peticion) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                ApiError.de("Esa operación no está permitida", "METODO_NO_PERMITIDO"), peticion);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> alFaltarUnParametro(MissingServletRequestParameterException e,
            HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION"), peticion);
    }

    private static ResponseEntity<ApiError> responder(HttpStatus estado, ApiError error, HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(error.conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
