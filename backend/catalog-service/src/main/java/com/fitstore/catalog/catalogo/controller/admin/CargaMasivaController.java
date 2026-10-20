package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.ResultadoCargaDto;
import com.fitstore.catalog.catalogo.dto.ValidacionCargaDto;
import com.fitstore.catalog.catalogo.service.CargaMasivaService;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.security.UsuarioActual;
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
@RequestMapping("/api/v1/admin/productos/carga-masiva")
@PreAuthorize("hasRole('ADMIN')")
public class CargaMasivaController {

    private final CargaMasivaService servicio;

    public CargaMasivaController(CargaMasivaService servicio) {
        this.servicio = servicio;
    }

    /** Devuelve el archivo tal cual, sin el envoltorio ApiResponse (contracts K4). */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaCargaMasiva(@RequestParam String formato) {
        CargaMasivaService.Archivo archivo = servicio.plantilla(formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType(archivo.tipo()))
                .body(archivo.contenido());
    }

    @PostMapping(path = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ValidacionCargaDto> validarCargaMasiva(@RequestParam("archivo") MultipartFile archivo) {
        return ApiResponse.ok(servicio.validar(archivo));
    }

    @PostMapping("/{validacionId}/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResultadoCargaDto> confirmarCargaMasiva(@PathVariable UUID validacionId, Authentication sesion) {
        return ApiResponse.ok(servicio.confirmar(validacionId, UUID.fromString(UsuarioActual.de(sesion).id())));
    }
}
