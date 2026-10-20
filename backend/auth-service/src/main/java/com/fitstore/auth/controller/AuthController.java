package com.fitstore.auth.controller;

import com.fitstore.auth.dto.request.RegistroRequest;
import com.fitstore.auth.dto.response.SesionResponse;
import com.fitstore.auth.security.CookieRefresh;
import com.fitstore.auth.service.RegistroService;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro, logins, sesión y recuperación de contraseña (micro 4.1). Sin lógica: delega en los servicios. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistroService registro;
    private final CookieRefresh cookie;

    public AuthController(RegistroService registro, CookieRefresh cookie) {
        this.registro = registro;
        this.cookie = cookie;
    }

    @PostMapping("/registro")
    @Operation(operationId = "registrarCliente", summary = "Registrar un cliente")
    public ResponseEntity<ApiResponse<SesionResponse>> registrar(@Valid @RequestBody RegistroRequest solicitud,
            HttpServletRequest peticion) {
        SesionEmitida sesion = registro.registrar(solicitud, ipDe(peticion));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/clientes/me")
                .header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.tokens()))
                .body(ApiResponse.ok("Cuenta creada", SesionResponse.de(sesion)));
    }

    /** IP del cliente: la primera de {@code X-Forwarded-For} (la pone el gateway) o la del socket. Máximo 45 caracteres. */
    static String ipDe(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        String ip = reenviada != null && !reenviada.isBlank() ? reenviada.split(",")[0].trim() : peticion.getRemoteAddr();
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
