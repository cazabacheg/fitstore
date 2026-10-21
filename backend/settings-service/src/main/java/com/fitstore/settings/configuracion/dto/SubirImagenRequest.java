package com.fitstore.settings.configuracion.dto;

import com.fitstore.settings.configuracion.model.TipoImagen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * Formulario multipart de subida de imágenes. Solo describe el cuerpo en el OpenAPI; el controlador recibe cada campo
 * por separado.
 */
public record SubirImagenRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Imagen: logos PNG o WebP (favicon también ICO) hasta 2 MB; landing JPG o WebP hasta 5 MB")
        MultipartFile archivo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TipoImagen tipo,
        @Size(max = 150) String textoAlternativo,
        @Schema(description = "Ruta de la tienda (/…) o URL https", maxLength = 300) String enlace) {
}
