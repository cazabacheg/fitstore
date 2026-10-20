package com.fitstore.auth.controller;

import static com.fitstore.auth.controller.ClienteController.idDe;

import com.fitstore.auth.dto.request.CambioPasswordRequest;
import com.fitstore.auth.dto.request.EditarStaffRequest;
import com.fitstore.auth.dto.request.InvitarStaffRequest;
import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.model.EstadoStaff;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.TipoCuenta;
import com.fitstore.auth.security.CookieRefresh;
import com.fitstore.auth.security.SesionEmitida;
import com.fitstore.auth.security.SoloAdmin;
import com.fitstore.auth.security.SoloStaff;
import com.fitstore.auth.service.Paginacion;
import com.fitstore.auth.service.PasswordService;
import com.fitstore.auth.service.StaffService;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Backoffice: cuenta propia del staff y administración del personal (ADMIN). */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private static final Set<String> ORDENABLES = Set.of("nombres", "apellidos", "email", "rol", "creadoEn", "ultimoLoginEn");

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

    @GetMapping
    @SoloAdmin
    @Operation(operationId = "listarStaff", summary = "Listar el personal")
    public ApiResponse<PaginaResponse<StaffResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RolStaff rol,
            @RequestParam(required = false) EstadoStaff estado,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) List<String> sort) {
        var pagina = Paginacion.de(page, size, sort, ORDENABLES, Sort.by(Sort.Direction.DESC, "creadoEn"));
        return ApiResponse.ok(personal.listar(q, rol, estado, pagina));
    }

    @PostMapping
    @SoloAdmin
    @Operation(operationId = "invitarStaff", summary = "Invitar a un miembro del staff")
    public ResponseEntity<ApiResponse<StaffResponse>> invitar(Authentication autenticacion,
            @Valid @RequestBody InvitarStaffRequest solicitud) {
        StaffResponse creado = personal.invitar(idDe(autenticacion), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/staff/" + creado.id())
                .body(ApiResponse.ok("Invitación enviada", creado));
    }

    @GetMapping("/{id}")
    @SoloAdmin
    @Operation(operationId = "obtenerStaff", summary = "Ver un miembro del staff")
    public ApiResponse<StaffResponse> obtener(@PathVariable UUID id) {
        return ApiResponse.ok(personal.obtener(id));
    }

    @PutMapping("/{id}")
    @SoloAdmin
    @Operation(operationId = "editarStaff", summary = "Editar nombre o rol")
    public ApiResponse<StaffResponse> editar(@PathVariable UUID id, @Valid @RequestBody EditarStaffRequest solicitud) {
        return ApiResponse.ok("Guardado", personal.editar(id, solicitud));
    }

    @PatchMapping("/{id}/desactivar")
    @SoloAdmin
    @Operation(operationId = "desactivarStaff", summary = "Desactivar a un miembro")
    public ApiResponse<StaffResponse> desactivar(Authentication autenticacion, @PathVariable UUID id) {
        return ApiResponse.ok("Miembro desactivado", personal.desactivar(id, idDe(autenticacion)));
    }

    @PatchMapping("/{id}/activar")
    @SoloAdmin
    @Operation(operationId = "activarStaff", summary = "Reactivar a un miembro")
    public ApiResponse<StaffResponse> activar(@PathVariable UUID id) {
        return ApiResponse.ok("Miembro activado", personal.activar(id));
    }

    @PostMapping("/{id}/reenviar-invitacion")
    @SoloAdmin
    @Operation(operationId = "reenviarInvitacionStaff", summary = "Reenviar la invitación")
    public ApiResponse<Void> reenviarInvitacion(@PathVariable UUID id) {
        personal.reenviarInvitacion(id);
        return ApiResponse.ok("Invitación reenviada");
    }
}
