package com.fitstore.settings.configuracion.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.settings.config.SolicitudInvalidaException;
import com.fitstore.settings.configuracion.dto.ConfiguracionAdminDto;
import com.fitstore.settings.configuracion.dto.ContenidoRequest;
import com.fitstore.settings.configuracion.dto.IdentidadRequest;
import com.fitstore.settings.configuracion.dto.ImagenRequest;
import com.fitstore.settings.configuracion.dto.ImagenTiendaDto;
import com.fitstore.settings.configuracion.dto.ParametrosRequest;
import com.fitstore.settings.configuracion.dto.SubirImagenRequest;
import com.fitstore.settings.configuracion.dto.TemaRequest;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.service.ConfiguracionService;
import com.fitstore.settings.configuracion.service.ImagenTiendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Edición de la configuración y de las imágenes de la tienda (solo ADMIN). */
@RestController
@RequestMapping(value = "/api/v1/admin/settings", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "configuracion-admin")
public class AdminSettingsController {

    private static final String ENLACE = "^(/(?!/)[^\\s]*|https://[^\\s]+)$";

    private final ConfiguracionService configuracion;
    private final ImagenTiendaService imagenes;

    public AdminSettingsController(ConfiguracionService configuracion, ImagenTiendaService imagenes) {
        this.configuracion = configuracion;
        this.imagenes = imagenes;
    }

    @GetMapping
    @Operation(operationId = "obtenerConfiguracionAdmin", summary = "Configuración completa (incluye lo no público)")
    public ApiResponse<ConfiguracionAdminDto> obtener() {
        return ApiResponse.ok(configuracion.obtenerAdmin());
    }

    @PutMapping("/identidad")
    @Operation(operationId = "guardarIdentidad", summary = "Guardar la identidad")
    public ApiResponse<ConfiguracionAdminDto> guardarIdentidad(@Valid @RequestBody IdentidadRequest solicitud,
            Authentication autenticacion) {
        return ApiResponse.ok("Guardamos la identidad", configuracion.guardarIdentidad(solicitud, actor(autenticacion)));
    }

    @PutMapping("/tema")
    @Operation(operationId = "guardarTema", summary = "Guardar el tema")
    public ApiResponse<ConfiguracionAdminDto> guardarTema(@Valid @RequestBody TemaRequest solicitud,
            Authentication autenticacion) {
        return ApiResponse.ok("Guardamos el tema", configuracion.guardarTema(solicitud, actor(autenticacion)));
    }

    @PutMapping("/contenido")
    @Operation(operationId = "guardarContenido",
            summary = "Guardar el contenido (hero, beneficios, contacto, redes, tienda física)")
    public ApiResponse<ConfiguracionAdminDto> guardarContenido(@Valid @RequestBody ContenidoRequest solicitud,
            Authentication autenticacion) {
        return ApiResponse.ok("Guardamos el contenido", configuracion.guardarContenido(solicitud, actor(autenticacion)));
    }

    @PutMapping("/parametros")
    @Operation(operationId = "guardarParametros", summary = "Guardar los parámetros del negocio")
    public ApiResponse<ConfiguracionAdminDto> guardarParametros(@Valid @RequestBody ParametrosRequest solicitud,
            Authentication autenticacion) {
        return ApiResponse.ok("Guardamos los parámetros",
                configuracion.guardarParametros(solicitud, actor(autenticacion)));
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "subirImagenTienda", summary = "Subir una imagen de la tienda",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SubirImagenRequest.class))))
    public ApiResponse<ImagenTiendaDto> subirImagen(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestPart("archivo") MultipartFile archivo,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestParam("tipo") TipoImagen tipo,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestParam(value = "textoAlternativo", required = false)
            @Size(max = 150, message = "El texto alternativo admite hasta 150 caracteres") String textoAlternativo,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestParam(value = "enlace", required = false)
            @Size(max = 300, message = "El enlace admite hasta 300 caracteres")
            @Pattern(regexp = ENLACE, message = "Usa una ruta de la tienda (/productos) o una URL https") String enlace) {
        try {
            return ApiResponse.ok("Guardamos la imagen",
                    imagenes.subir(tipo, archivo.getBytes(), textoAlternativo, enlace));
        } catch (IOException e) {
            throw new SolicitudInvalidaException("archivo", "No pudimos leer el archivo");
        }
    }

    @PutMapping("/imagenes/{id}")
    @Operation(operationId = "actualizarImagenTienda", summary = "Cambiar orden, texto alternativo o enlace")
    public ApiResponse<ImagenTiendaDto> actualizarImagen(@PathVariable Long id,
            @Valid @RequestBody ImagenRequest solicitud) {
        return ApiResponse.ok("Guardamos la imagen", imagenes.actualizar(id, solicitud));
    }

    @DeleteMapping("/imagenes/{id}")
    @Operation(operationId = "eliminarImagenTienda", summary = "Eliminar una imagen")
    public ApiResponse<Void> eliminarImagen(@PathVariable Long id) {
        imagenes.eliminar(id);
        return ApiResponse.ok("Eliminamos la imagen");
    }

    private static String actor(Authentication autenticacion) {
        return UsuarioActual.de(autenticacion).id();
    }
}
