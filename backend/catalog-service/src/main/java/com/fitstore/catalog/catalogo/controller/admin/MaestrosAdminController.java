package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.ColorRequest;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.dto.TallaRequest;
import com.fitstore.catalog.catalogo.service.MaestrosService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tallas y colores maestros del backoffice (micro 4.1): solo ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class MaestrosAdminController {

    private final MaestrosService servicio;

    public MaestrosAdminController(MaestrosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tallas")
    public ApiResponse<List<TallaDto>> listarTallasAdmin() {
        return ApiResponse.ok(servicio.listarTallas());
    }

    @PostMapping("/tallas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TallaDto> crearTalla(@Valid @RequestBody TallaRequest peticion) {
        return ApiResponse.ok(servicio.crearTalla(peticion));
    }

    @PutMapping("/tallas/{id}")
    public ApiResponse<TallaDto> editarTalla(@PathVariable Long id, @Valid @RequestBody TallaRequest peticion) {
        return ApiResponse.ok(servicio.editarTalla(id, peticion));
    }

    @DeleteMapping("/tallas/{id}")
    public ApiResponse<Void> eliminarTalla(@PathVariable Long id) {
        servicio.eliminarTalla(id);
        return ApiResponse.ok("Talla eliminada");
    }

    @GetMapping("/colores")
    public ApiResponse<List<ColorDto>> listarColoresAdmin() {
        return ApiResponse.ok(servicio.listarColores());
    }

    @PostMapping("/colores")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ColorDto> crearColor(@Valid @RequestBody ColorRequest peticion) {
        return ApiResponse.ok(servicio.crearColor(peticion));
    }

    @PutMapping("/colores/{id}")
    public ApiResponse<ColorDto> editarColor(@PathVariable Long id, @Valid @RequestBody ColorRequest peticion) {
        return ApiResponse.ok(servicio.editarColor(id, peticion));
    }

    @DeleteMapping("/colores/{id}")
    public ApiResponse<Void> eliminarColor(@PathVariable Long id) {
        servicio.eliminarColor(id);
        return ApiResponse.ok("Color eliminado");
    }
}
