package com.fitstore.auth.security;

import com.fitstore.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/** 401 y 403 de los filtros de seguridad con el formato {@link ApiError} (K5). */
public class RespuestasSeguridadAuth implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    public RespuestasSeguridadAuth(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException e)
            throws IOException {
        escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED,
                ApiError.de("Tu sesión venció, vuelve a ingresar", "NO_AUTENTICADO"));
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException e)
            throws IOException {
        escribir(peticion, respuesta, HttpStatus.FORBIDDEN,
                ApiError.de("No tienes permiso para esta acción", "SIN_PERMISO"));
    }

    private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta, HttpStatus estado, ApiError error)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), error.conRequestId(peticion.getHeader("X-Request-Id")));
    }
}
