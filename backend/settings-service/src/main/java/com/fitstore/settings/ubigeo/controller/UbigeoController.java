package com.fitstore.settings.ubigeo.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.settings.ubigeo.dto.DistritoEncontradoDto;
import com.fitstore.settings.ubigeo.dto.UbigeoItemDto;
import com.fitstore.settings.ubigeo.dto.UbigeoNombreDto;
import com.fitstore.settings.ubigeo.service.UbigeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública del ubigeo del INEI. Las listas llevan caché de 24 h también en el navegador (CA-3-32). */
@RestController
@RequestMapping(value = "/api/v1/ubigeo", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "ubigeo")
public class UbigeoController {

    private static final CacheControl CACHE_24_HORAS = CacheControl.maxAge(Duration.ofHours(24)).cachePublic();

    private final UbigeoService servicio;

    public UbigeoController(UbigeoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/departamentos")
    @Operation(operationId = "listarDepartamentos", summary = "Departamentos")
    public ResponseEntity<ApiResponse<List<UbigeoItemDto>>> departamentos() {
        return conCache(servicio.departamentos());
    }

    @GetMapping("/departamentos/{codigo}/provincias")
    @Operation(operationId = "listarProvincias", summary = "Provincias de un departamento")
    public ResponseEntity<ApiResponse<List<UbigeoItemDto>>> provincias(
            @PathVariable @Pattern(regexp = "^[0-9]{2}$") String codigo) {
        return conCache(servicio.provincias(codigo));
    }

    @GetMapping("/provincias/{codigo}/distritos")
    @Operation(operationId = "listarDistritos", summary = "Distritos de una provincia")
    public ResponseEntity<ApiResponse<List<UbigeoItemDto>>> distritos(
            @PathVariable @Pattern(regexp = "^[0-9]{4}$") String codigo) {
        return conCache(servicio.distritos(codigo));
    }

    @GetMapping("/distritos")
    @Operation(operationId = "buscarDistritos", summary = "Buscar distritos por nombre")
    public ApiResponse<List<DistritoEncontradoDto>> buscar(
            @RequestParam @NotBlank @Size(min = 3, max = 60) String q) {
        return ApiResponse.ok(servicio.buscarDistritos(q));
    }

    @GetMapping("/{codigo}")
    @Operation(operationId = "obtenerUbigeo", summary = "Nombre completo de un código")
    public ApiResponse<UbigeoNombreDto> nombre(
            @PathVariable @Pattern(regexp = "^([0-9]{2}|[0-9]{4}|[0-9]{6})$") String codigo) {
        return ApiResponse.ok(servicio.nombre(codigo));
    }

    private static <T> ResponseEntity<ApiResponse<T>> conCache(T datos) {
        return ResponseEntity.ok().cacheControl(CACHE_24_HORAS).body(ApiResponse.ok(datos));
    }
}
