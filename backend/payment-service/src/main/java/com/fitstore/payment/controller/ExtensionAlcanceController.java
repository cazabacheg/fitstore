package com.fitstore.payment.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.payment.dto.ReembolsoDto;
import com.fitstore.payment.dto.ResolverManualRequest;
import com.fitstore.payment.model.EstadoReembolso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas del contrato de {@code payment-service} que son <strong>extensión de alcance</strong> (HU-5-10, HU-5-12, HU-5-03):
 * reembolsos del backoffice y webhook de Culqi. Tienen la misma firma que el contrato (para que {@code oasdiff} no las
 * marque como faltantes) pero no hacen nada: responden 501 {@code EXTENSION_DE_ALCANCE}. Se construyen cuando la
 * historia salga de {@code docs/implementacion/extension-alcance.md}.
 */
@RestController
@Validated
public class ExtensionAlcanceController {

    private static FitstoreException extensionDeAlcance() {
        return new FitstoreException(HttpStatus.NOT_IMPLEMENTED, "EXTENSION_DE_ALCANCE",
                "Esta función todavía no está disponible");
    }

    @GetMapping(path = "/api/v1/admin/reembolsos", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaginaResponse<ReembolsoDto>> listarReembolsos(
            @RequestParam(required = false) List<EstadoReembolso> estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        throw extensionDeAlcance();
    }

    @PostMapping(path = "/api/v1/admin/reembolsos/{id}/reintentar", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReembolsoDto> reintentarReembolso(@PathVariable UUID id) {
        throw extensionDeAlcance();
    }

    @PatchMapping(path = "/api/v1/admin/reembolsos/{id}/resuelto-manual", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReembolsoDto> resolverReembolsoManual(@PathVariable UUID id,
            @Valid @RequestBody ResolverManualRequest solicitud) {
        throw extensionDeAlcance();
    }

    @PostMapping(path = "/api/v1/pagos/webhooks/culqi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> recibirWebhookCulqi(@RequestBody Map<String, Object> cuerpo) {
        throw extensionDeAlcance();
    }
}
