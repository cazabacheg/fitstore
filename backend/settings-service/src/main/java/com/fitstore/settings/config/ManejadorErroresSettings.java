package com.fitstore.settings.config;

import com.fitstore.common.api.ApiError;
import com.fitstore.common.api.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/** Errores de settings: los comunes de {@code fitstore-common} más validación por campo y versión desactualizada. */
@RestControllerAdvice
public class ManejadorErroresSettings extends GlobalExceptionHandlerBase {

    public static final String MENSAJE_VERSION = "Otro administrador modificó la configuración; recarga la página";

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> alSerInvalida(SolicitudInvalidaException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.de(MENSAJE_VALIDACION, "VALIDACION").conErrores(e.errores()), peticion);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> alPisarseLaVersion(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT, ApiError.de(MENSAJE_VERSION, "CONFLICTO_VERSION"), peticion);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> alViolarUnaRestriccion(DataIntegrityViolationException e,
            HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT,
                ApiError.de("El dato ya fue modificado por otra persona; vuelve a intentarlo", "CONFLICTO"), peticion);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> alFallarLaSubida(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION"), peticion);
    }

    private static ResponseEntity<ApiError> responder(HttpStatus estado, ApiError error, HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(error.conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
