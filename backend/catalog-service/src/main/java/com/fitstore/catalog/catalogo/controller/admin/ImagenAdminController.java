package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.ImagenProductoRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import com.fitstore.catalog.catalogo.service.ImagenService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Imágenes de producto del backoffice (micro 4.1): solo ADMIN. */
@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class ImagenAdminController {

    private final ImagenService servicio;

    public ImagenAdminController(ImagenService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(path = "/productos/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductoAdminDto> subirImagenProducto(@PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "colorId", required = false) Long colorId,
            @RequestParam(value = "principal", defaultValue = "false") boolean principal) {
        return ApiResponse.ok(servicio.subir(id, archivo, colorId, principal));
    }

    @PutMapping("/imagenes/{id}")
    public ApiResponse<ProductoAdminDto> editarImagenProducto(@PathVariable Long id,
            @Valid @RequestBody ImagenProductoRequest peticion) {
        return ApiResponse.ok(servicio.editar(id, peticion));
    }

    @DeleteMapping("/imagenes/{id}")
    public ApiResponse<ProductoAdminDto> eliminarImagenProducto(@PathVariable Long id) {
        return ApiResponse.ok(servicio.eliminar(id));
    }
}
