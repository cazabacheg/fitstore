package com.fitstore.catalog.catalogo.controller;

import com.fitstore.catalog.catalogo.dto.CotizacionDto;
import com.fitstore.catalog.catalogo.dto.CotizarRequest;
import com.fitstore.catalog.catalogo.service.CotizacionService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cotización pública (HU-2-03): la usan order (carrito y checkout) y la tienda. No modifica nada. */
@RestController
@RequestMapping("/api/v1/precios")
public class CotizacionController {

    private final CotizacionService servicio;

    public CotizacionController(CotizacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/cotizar")
    public ApiResponse<CotizacionDto> cotizarLineas(@Valid @RequestBody CotizarRequest peticion) {
        return ApiResponse.ok(servicio.cotizar(peticion.lineas()));
    }
}
