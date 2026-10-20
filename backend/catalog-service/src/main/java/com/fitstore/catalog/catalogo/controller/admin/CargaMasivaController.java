package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.ResultadoCargaDto;
import com.fitstore.catalog.catalogo.dto.ValidacionCargaDto;
import com.fitstore.catalog.catalogo.service.CargaMasivaService;
import com.fitstore.common.security.UsuarioActual;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Carga masiva de productos del backoffice (micro 4.4.1): solo ADMIN. */
@RestController
@RequestMapping(path = "/api/v1/admin/productos/carga-masiva", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class CargaMasivaController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CargaMasivaService servicio;

    public CargaMasivaController(CargaMasivaService servicio) {
        this.servicio = servicio;
    }

    /** Devuelve el archivo tal cual, sin el envoltorio ApiResponse (contracts K4). */
    @GetMapping(path = "/plantilla", produces = {XLSX, "text/csv"})
    @ApiResponse(responseCode = "200", description = "Plantilla.", content = {
            @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary"))})
    public ResponseEntity<byte[]> descargarPlantillaCargaMasiva(
            @RequestParam @Schema(allowableValues = {"xlsx", "csv"}) String formato) {
        CargaMasivaService.Archivo archivo = servicio.plantilla(formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType(archivo.tipo()))
                .body(archivo.contenido());
    }

    @PostMapping(path = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public com.fitstore.common.api.ApiResponse<ValidacionCargaDto> validarCargaMasiva(@RequestParam("archivo") MultipartFile archivo) {
        return com.fitstore.common.api.ApiResponse.ok(servicio.validar(archivo));
    }

    @PostMapping("/{validacionId}/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public com.fitstore.common.api.ApiResponse<ResultadoCargaDto> confirmarCargaMasiva(@PathVariable UUID validacionId, Authentication sesion) {
        return com.fitstore.common.api.ApiResponse.ok(servicio.confirmar(validacionId, UUID.fromString(UsuarioActual.de(sesion).id())));
    }
}
