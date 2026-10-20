package com.fitstore.auth.controller;

import static com.fitstore.auth.controller.ClienteController.idDe;

import com.fitstore.auth.dto.request.CambioPasswordRequest;
import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CookieRefresh;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.SoloStaff;
import com.fitstore.auth.service.PasswordService;
import com.fitstore.auth.service.StaffService;
import com.fitstore.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backoffice: cuenta propia del staff y administración del personal (ADMIN). */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService personal;
    private final PasswordService passwords;
    private final CookieRefresh cookie;

    public StaffController(StaffService personal, PasswordService passwords, CookieRefresh cookie) {
        this.personal = personal;
        this.passwords = passwords;
        this.cookie = cookie;
    }

    @GetMapping("/me")
    @SoloStaff
    @Operation(operationId = "obtenerMiPerfilStaff", summary = "Ver mi perfil de staff")
    public ApiResponse<StaffResponse> miPerfil(Authentication autenticacion) {
        return ApiResponse.ok(personal.miPerfil(idDe(autenticacion)));
    }

    @PutMapping("/me")
    @SoloStaff
    @Operation(operationId = "actualizarMiPerfilStaff", summary = "Editar mi nombre")
    public ApiResponse<StaffResponse> actualizarMiPerfil(Authentication autenticacion,
            @Valid @RequestBody PerfilStaffRequest solicitud) {
        return ApiResponse.ok("Guardado", personal.actualizarMiPerfil(idDe(autenticacion), solicitud));
    }

    @PutMapping("/me/password")
    @SoloStaff
    @Operation(operationId = "cambiarMiPasswordStaff", summary = "Cambiar mi contraseña de staff")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(Authentication autenticacion,
            @Valid @RequestBody CambioPasswordRequest solicitud) {
        SesionEmitida propia = passwords.cambiar(TipoCuenta.STAFF, idDe(autenticacion),
                solicitud.passwordActual(), solicitud.nuevaPassword());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(propia.tokens()))
                .body(ApiResponse.ok("Contraseña actualizada"));
    }
}
