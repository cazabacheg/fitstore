package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.CategoriaAdminDto;
import com.fitstore.catalog.catalogo.dto.CategoriaRequest;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.dto.OrdenRequest;
import com.fitstore.catalog.catalogo.service.CategoriaService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Categorías y guía de tallas del backoffice (micro 4.1): solo ADMIN. */
@RestController
@RequestMapping(path = "/api/v1/admin/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaAdminController {

    private final CategoriaService servicio;

    public CategoriaAdminController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ApiResponse<List<CategoriaAdminDto>> listarCategoriasAdmin() {
        return ApiResponse.ok(servicio.listar());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoriaAdminDto> crearCategoria(@Valid @RequestBody CategoriaRequest peticion) {
        return ApiResponse.ok(servicio.crear(peticion));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoriaAdminDto> editarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest peticion) {
        return ApiResponse.ok(servicio.editar(id, peticion));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminarCategoria(@PathVariable Long id) {
        servicio.eliminar(id);
        return ApiResponse.ok("Categoría eliminada");
    }

    @PatchMapping("/{id}/desactivar")
    public ApiResponse<CategoriaAdminDto> desactivarCategoria(@PathVariable Long id) {
        return ApiResponse.ok(servicio.desactivar(id));
    }

    @PatchMapping("/{id}/activar")
    public ApiResponse<CategoriaAdminDto> activarCategoria(@PathVariable Long id) {
        return ApiResponse.ok(servicio.activar(id));
    }

    @PatchMapping("/{id}/orden")
    public ApiResponse<List<CategoriaAdminDto>> ordenarCategoria(@PathVariable Long id, @Valid @RequestBody OrdenRequest peticion) {
        return ApiResponse.ok(servicio.ordenar(id, peticion.orden()));
    }

    @PutMapping("/{id}/guia-tallas")
    public ApiResponse<GuiaTallasDto> guardarGuiaTallas(@PathVariable Long id, @Valid @RequestBody GuiaTallasDto peticion) {
        return ApiResponse.ok(servicio.guardarGuiaTallas(id, peticion));
    }
}
