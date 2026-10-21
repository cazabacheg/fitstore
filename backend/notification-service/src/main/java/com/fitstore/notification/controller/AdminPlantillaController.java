package com.fitstore.notification.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.notification.dto.EnvioDto;
import com.fitstore.notification.dto.PlantillaDto;
import com.fitstore.notification.dto.PlantillaRequest;
import com.fitstore.notification.dto.TextosPlantillaRequest;
import com.fitstore.notification.dto.VistaPreviaDto;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Editor de plantillas (HU-7-06, P-7-02 y P-7-03): extensión de alcance. Conserva la firma del contrato y responde
 * 501 {@code EXTENSION_DE_ALCANCE} hasta que se construya (docs/implementacion/extension-alcance.md).
 */
@RestController
@RequestMapping(value = "/api/v1/admin/notificaciones/plantillas", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "plantillas")
@SecurityRequirement(name = "bearerAuth")
public class AdminPlantillaController {

    @GetMapping
    @Operation(operationId = "listarPlantillas", summary = "Catálogo de plantillas")
    public ApiResponse<List<PlantillaDto>> listar() {
        throw ExtensionDeAlcance.de("El catálogo de plantillas");
    }

    @GetMapping("/{codigo}")
    @Operation(operationId = "obtenerPlantilla", summary = "Ver una plantilla")
    public ApiResponse<PlantillaDto> obtener(@PathVariable CatalogoPlantillas codigo) {
        throw ExtensionDeAlcance.de("Ver una plantilla");
    }

    @PutMapping("/{codigo}")
    @Operation(operationId = "guardarPlantilla", summary = "Guardar los textos y activar o desactivar")
    public ApiResponse<PlantillaDto> guardar(@PathVariable CatalogoPlantillas codigo,
            @Valid @RequestBody PlantillaRequest solicitud) {
        throw ExtensionDeAlcance.de("Editar una plantilla");
    }

    @PostMapping("/{codigo}/restaurar")
    @Operation(operationId = "restaurarPlantilla", summary = "Volver a los textos predeterminados")
    public ApiResponse<PlantillaDto> restaurar(@PathVariable CatalogoPlantillas codigo) {
        throw ExtensionDeAlcance.de("Restaurar una plantilla");
    }

    @PostMapping("/{codigo}/vista-previa")
    @Operation(operationId = "previsualizarPlantilla", summary = "Vista previa con los textos del editor")
    public ApiResponse<VistaPreviaDto> previsualizar(@PathVariable CatalogoPlantillas codigo,
            @Valid @RequestBody TextosPlantillaRequest textos) {
        throw ExtensionDeAlcance.de("La vista previa de una plantilla");
    }

    @PostMapping("/{codigo}/prueba")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "enviarPruebaPlantilla", summary = "Enviar una prueba a mi correo")
    public ApiResponse<EnvioDto> enviarPrueba(@PathVariable CatalogoPlantillas codigo,
            @Valid @RequestBody TextosPlantillaRequest textos) {
        throw ExtensionDeAlcance.de("Enviar una prueba de plantilla");
    }
}
