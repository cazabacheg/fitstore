package com.fitstore.catalog.config;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.ErrorCampo;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Errores K5 de catalog: suma a la base los de subida de archivos, validación de parámetros y conflictos de datos. */
@RestControllerAdvice
public class ManejadorErroresCatalog extends GlobalExceptionHandlerBase {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> alExcederElTamano(MaxUploadSizeExceededException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de("El archivo supera los 5 MB permitidos", "ARCHIVO_MUY_GRANDE"), peticion);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> alFaltarUnDato(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION"), peticion);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> alValidarParametros(HandlerMethodValidationException e, HttpServletRequest peticion) {
        List<ErrorCampo> errores = e.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(err -> new ErrorCampo(String.valueOf(r.getMethodParameter().getParameterName()),
                                String.valueOf(err.getDefaultMessage()))))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION").conErrores(errores), peticion);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> alChocarLaVersion(ObjectOptimisticLockingFailureException e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT,
                ApiError.de("Otro usuario modificó este registro; recarga la página", "CONFLICTO_VERSION"), peticion);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> alViolarUnaRestriccion(DataIntegrityViolationException e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT,
                ApiError.de("Ya existe un registro con esos datos o está en uso", "CONFLICTO"), peticion);
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
