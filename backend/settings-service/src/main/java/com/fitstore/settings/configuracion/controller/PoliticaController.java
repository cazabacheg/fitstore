package com.fitstore.settings.configuracion.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.settings.configuracion.dto.PoliticaDto;
import com.fitstore.settings.configuracion.dto.PublicarPoliticaRequest;
import com.fitstore.settings.configuracion.dto.VersionPoliticaDto;
import com.fitstore.settings.configuracion.service.PoliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lectura pública de las políticas y publicación de versiones nuevas por el ADMIN. */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "politicas")
public class PoliticaController {

    private final PoliticaService politicas;

    public PoliticaController(PoliticaService politicas) {
        this.politicas = politicas;
    }

    @GetMapping("/settings/politicas/{tipo}")
    @Operation(operationId = "obtenerPolitica", summary = "Texto de una política")
    public ApiResponse<PoliticaDto> obtener(@PathVariable String tipo,
            @RequestParam(value = "version", required = false) @Min(1) Integer version) {
        return ApiResponse.ok(politicas.obtener(tipo, version));
    }

    @GetMapping("/admin/settings/politicas/{tipo}/versiones")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "listarVersionesPolitica", summary = "Historial de versiones de una política")
    public ApiResponse<List<VersionPoliticaDto>> versiones(@PathVariable String tipo) {
        return ApiResponse.ok(politicas.versiones(tipo));
    }

    @PostMapping("/admin/settings/politicas/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "publicarPolitica", summary = "Publicar una nueva versión")
    public ApiResponse<PoliticaDto> publicar(@PathVariable String tipo,
            @Valid @RequestBody PublicarPoliticaRequest solicitud, Authentication autenticacion) {
        return ApiResponse.ok("Publicamos la nueva versión",
                politicas.publicar(tipo, solicitud.contenido(), UsuarioActual.de(autenticacion).id()));
    }
}
