package com.fitstore.payment.config;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores K5 de payment: suma a la base los de cabeceras o parámetros que faltan y métodos no permitidos. */
@RestControllerAdvice
public class ManejadorErroresPayment extends GlobalExceptionHandlerBase {

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> alFaltarUnDato(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION"), peticion);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> alUsarOtroMetodo(HttpRequestMethodNotSupportedException e, HttpServletRequest peticion) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, ApiError.de("Método no permitido", "METODO_NO_PERMITIDO"), peticion);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> alUsarOtroTipo(HttpMediaTypeNotSupportedException e, HttpServletRequest peticion) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiError.de("Tipo de contenido no soportado", "TIPO_NO_SOPORTADO"), peticion);
    }

    private static ResponseEntity<ApiError> responder(HttpStatus estado, ApiError error, HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(error.conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
