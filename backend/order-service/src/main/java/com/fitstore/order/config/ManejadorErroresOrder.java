package com.fitstore.order.config;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores de order (K5): lo de la base más el conflicto de versión (otro operador o un evento cambió el pedido a la vez,
 * CA-4-48) y las cabeceras que faltan, que responden 400 y no 500.
 */
@RestControllerAdvice
public class ManejadorErroresOrder extends GlobalExceptionHandlerBase {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> alCambiarElPedidoAlMismoTiempo(OptimisticLockingFailureException e,
            HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError
                .de("El pedido cambió; recarga la página", "CONFLICTO_VERSION")
                .conRequestId(peticion.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> alFaltarUnaCabecera(MissingRequestHeaderException e, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError
                .de("Falta la cabecera " + e.getHeaderName(), "VALIDACION")
                .conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
