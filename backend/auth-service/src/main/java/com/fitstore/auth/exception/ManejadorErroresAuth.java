package com.fitstore.auth.exception;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores K5 de auth: agrega los 400 con campo del servicio y el conflicto de versión (bloqueo optimista). */
@RestControllerAdvice
public class ManejadorErroresAuth extends GlobalExceptionHandlerBase {

    @ExceptionHandler(ErrorCamposException.class)
    public ResponseEntity<ApiError> alFallarCampos(ErrorCamposException e, HttpServletRequest peticion) {
        ApiError error = ApiError.de(e.getMessage(), e.codigo()).conErrores(e.errores())
                .conRequestId(peticion.getHeader("X-Request-Id"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> alConflictoDeVersion(OptimisticLockingFailureException e, HttpServletRequest peticion) {
        ApiError error = ApiError.de("Otro usuario modificó este registro; recarga la página", "CONFLICTO_VERSION")
                .conRequestId(peticion.getHeader("X-Request-Id"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
