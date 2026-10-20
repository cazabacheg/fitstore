package com.fitstore.auth.controller;

import com.fitstore.auth.dto.request.CambioPasswordRequest;
import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteResponse;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CookieRefresh;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.SoloCliente;
import com.fitstore.auth.service.ClienteService;
import com.fitstore.auth.service.PasswordService;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cuenta propia del cliente: perfil, contraseña y direcciones. El id sale siempre del token, nunca de la URL. */
@RestController
@RequestMapping("/api/v1/clientes/me")
@SoloCliente
public class ClienteController {

    private final ClienteService clientes;
    private final PasswordService passwords;
    private final CookieRefresh cookie;

    public ClienteController(ClienteService clientes, PasswordService passwords, CookieRefresh cookie) {
        this.clientes = clientes;
        this.passwords = passwords;
        this.cookie = cookie;
    }

    @GetMapping
    @Operation(operationId = "obtenerMiPerfilCliente", summary = "Ver mi perfil de cliente")
    public ApiResponse<ClienteResponse> miPerfil(Authentication autenticacion) {
        return ApiResponse.ok(clientes.perfil(idDe(autenticacion)));
    }

    @PutMapping
    @Operation(operationId = "actualizarMiPerfilCliente", summary = "Editar mi perfil (nombres, apellidos, celular)")
    public ApiResponse<ClienteResponse> actualizarPerfil(Authentication autenticacion,
            @Valid @RequestBody PerfilClienteRequest solicitud) {
        return ApiResponse.ok("Guardado", clientes.actualizarPerfil(idDe(autenticacion), solicitud));
    }

    @PutMapping("/password")
    @Operation(operationId = "cambiarMiPasswordCliente", summary = "Cambiar mi contraseña")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(Authentication autenticacion,
            @Valid @RequestBody CambioPasswordRequest solicitud) {
        SesionEmitida propia = passwords.cambiar(TipoCuenta.CLIENTE, idDe(autenticacion),
                solicitud.passwordActual(), solicitud.nuevaPassword());
        // las demás sesiones quedan cerradas; la que cambió la contraseña sigue con un refresh nuevo
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(propia.tokens()))
                .body(ApiResponse.ok("Contraseña actualizada"));
    }

    static UUID idDe(Authentication autenticacion) {
        return UUID.fromString(UsuarioActual.de(autenticacion).id());
    }
}
