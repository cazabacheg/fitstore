package com.fitstore.auth.controller;

import com.fitstore.auth.dto.request.LoginRequest;
import com.fitstore.auth.dto.request.OlvidoRequest;
import com.fitstore.auth.dto.request.RegistroRequest;
import com.fitstore.auth.dto.request.RestablecerRequest;
import com.fitstore.auth.dto.response.SesionResponse;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CookieRefresh;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.service.AutenticacionService;
import com.fitstore.auth.service.PasswordService;
import com.fitstore.auth.service.RegistroService;
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

    private static final String MENSAJE_OLVIDO = "Si el correo está registrado, te enviaremos un enlace";

    private final RegistroService registro;
    private final AutenticacionService autenticacion;
    private final PasswordService passwords;
    private final CookieRefresh cookie;

    public AuthController(RegistroService registro, AutenticacionService autenticacion, PasswordService passwords,
            CookieRefresh cookie) {
        this.registro = registro;
        this.autenticacion = autenticacion;
        this.passwords = passwords;
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

    @PostMapping("/login")
    @Operation(operationId = "loginCliente", summary = "Iniciar sesión como cliente (tienda)")
    public ResponseEntity<ApiResponse<SesionResponse>> login(@Valid @RequestBody LoginRequest solicitud) {
        return sesion(autenticacion.iniciarSesion(TipoCuenta.CLIENTE, solicitud.email(), solicitud.password()));
    }

    @PostMapping("/staff/login")
    @Operation(operationId = "loginStaff", summary = "Iniciar sesión como staff (backoffice)")
    public ResponseEntity<ApiResponse<SesionResponse>> loginStaff(@Valid @RequestBody LoginRequest solicitud) {
        return sesion(autenticacion.iniciarSesion(TipoCuenta.STAFF, solicitud.email(), solicitud.password()));
    }

    @PostMapping("/refresh")
    @Operation(operationId = "refrescarSesion", summary = "Renovar el access token")
    public ResponseEntity<ApiResponse<SesionResponse>> refrescar(HttpServletRequest peticion) {
        return sesion(autenticacion.refrescar(cookie.leer(peticion)));
    }

    @PostMapping("/logout")
    @Operation(operationId = "cerrarSesion", summary = "Cerrar la sesión")
    public ResponseEntity<ApiResponse<Void>> cerrarSesion(HttpServletRequest peticion) {
        autenticacion.cerrarSesion(cookie.leer(peticion));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.borrar())
                .body(ApiResponse.ok("Sesión cerrada"));
    }

    @PostMapping("/password/olvido")
    @Operation(operationId = "olvidoPasswordCliente", summary = "Pedir enlace de recuperación (cliente)")
    public ApiResponse<Void> olvidoCliente(@Valid @RequestBody OlvidoRequest solicitud) {
        passwords.solicitarRecuperacion(TipoCuenta.CLIENTE, solicitud.email());
        return ApiResponse.ok(MENSAJE_OLVIDO);
    }

    @PostMapping("/staff/password/olvido")
    @Operation(operationId = "olvidoPasswordStaff", summary = "Pedir enlace de recuperación (staff)")
    public ApiResponse<Void> olvidoStaff(@Valid @RequestBody OlvidoRequest solicitud) {
        passwords.solicitarRecuperacion(TipoCuenta.STAFF, solicitud.email());
        return ApiResponse.ok(MENSAJE_OLVIDO);
    }

    @PostMapping("/password/restablecer")
    @Operation(operationId = "restablecerPassword", summary = "Crear una nueva contraseña con el enlace")
    public ApiResponse<Void> restablecer(@Valid @RequestBody RestablecerRequest solicitud) {
        passwords.restablecer(solicitud.token(), solicitud.nuevaPassword());
        return ApiResponse.ok("Contraseña actualizada");
    }

    private ResponseEntity<ApiResponse<SesionResponse>> sesion(SesionEmitida sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.tokens()))
                .body(ApiResponse.ok("Sesión iniciada", SesionResponse.de(sesion)));
    }

    /** IP del cliente: la primera de {@code X-Forwarded-For} (la pone el gateway) o la del socket. Máximo 45 caracteres. */
    static String ipDe(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        String ip = reenviada != null && !reenviada.isBlank() ? reenviada.split(",")[0].trim() : peticion.getRemoteAddr();
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
