package com.fitstore.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte las excepciones en {@link ApiError} (contracts K5). Cada servicio lo extiende con un
 * {@code @RestControllerAdvice} propio si necesita más casos, o usa el que registra la autoconfiguración.
 */
public abstract class GlobalExceptionHandlerBase {

    public static final String MENSAJE_VALIDACION = "Revisa los datos ingresados";
    public static final String MENSAJE_ERROR_INTERNO = "Ocurrió un error inesperado, inténtalo en un momento";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandlerBase.class);

    @ExceptionHandler(FitstoreException.class)
    public ResponseEntity<ApiError> alNegocio(FitstoreException e, HttpServletRequest peticion) {
        ApiError error = ApiError.de(e.getMessage(), e.codigo()).conDetalles(e.detalles());
        return responder(e.estado(), error, peticion);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> alValidar(MethodArgumentNotValidException e, HttpServletRequest peticion) {
        List<ErrorCampo> errores = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorCampo(f.getField(), f.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.de(MENSAJE_VALIDACION, "VALIDACION").conErrores(errores), peticion);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> alViolarRestricciones(ConstraintViolationException e, HttpServletRequest peticion) {
        List<ErrorCampo> errores = e.getConstraintViolations().stream()
                .map(v -> new ErrorCampo(ultimoNodo(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST,
                ApiError.de(MENSAJE_VALIDACION, "VALIDACION").conErrores(errores), peticion);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> alLeerLaPeticion(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.de(MENSAJE_VALIDACION, "VALIDACION"), peticion);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> alDenegarAcceso(AccessDeniedException e, HttpServletRequest peticion) {
        return responder(HttpStatus.FORBIDDEN,
                ApiError.de("No tienes permiso para esta acción", "SIN_PERMISO"), peticion);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> alFallarLaAutenticacion(AuthenticationException e, HttpServletRequest peticion) {
        return responder(HttpStatus.UNAUTHORIZED,
                ApiError.de("Tu sesión venció, vuelve a ingresar", "NO_AUTENTICADO"), peticion);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> alNoExistirLaRuta(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND,
                ApiError.de("No encontramos lo que buscas", RecursoNoEncontradoException.CODIGO), peticion);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> alFallarInesperadamente(Exception e, HttpServletRequest peticion) {
        LOG.error("Error inesperado en {} {}", peticion.getMethod(), peticion.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                ApiError.de(MENSAJE_ERROR_INTERNO, "ERROR_INTERNO"), peticion);
    }

    private static ResponseEntity<ApiError> responder(HttpStatus estado, ApiError error, HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(error.conRequestId(peticion.getHeader("X-Request-Id")));
    }

    private static String ultimoNodo(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto >= 0 ? ruta.substring(punto + 1) : ruta;
    }
}
