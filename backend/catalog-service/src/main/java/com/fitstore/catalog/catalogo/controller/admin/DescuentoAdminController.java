package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.PoliticaDescuentosDto;
import com.fitstore.catalog.catalogo.service.DescuentoService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Política global de descuentos por volumen (micro 4.1): solo ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/descuentos")
@PreAuthorize("hasRole('ADMIN')")
public class DescuentoAdminController {

    private final DescuentoService servicio;

    public DescuentoAdminController(DescuentoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ApiResponse<PoliticaDescuentosDto> obtenerPoliticaDescuentos() {
        return ApiResponse.ok(servicio.obtenerGlobal());
    }

    @PutMapping
    public ApiResponse<PoliticaDescuentosDto> guardarPoliticaDescuentos(@Valid @RequestBody PoliticaDescuentosDto peticion) {
        return ApiResponse.ok(servicio.guardarGlobal(peticion));
    }
}
