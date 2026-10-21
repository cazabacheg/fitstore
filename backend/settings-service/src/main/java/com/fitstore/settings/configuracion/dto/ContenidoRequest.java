package com.fitstore.settings.configuracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Guardar el contenido de la tienda. El enlace del hero es una ruta de la tienda ("/productos?…") o una URL https:
 * nunca {@code javascript:} ni rutas relativas al protocolo (S5).
 */
public record ContenidoRequest(
        @NotBlank(message = "Indica el título del hero")
        @Size(min = 2, max = 80, message = "El título admite de 2 a 80 caracteres") String heroTitulo,
        @Size(max = 160, message = "El subtítulo admite hasta 160 caracteres") String heroSubtitulo,
        @Size(max = 30, message = "El texto del botón admite hasta 30 caracteres") String heroCtaTexto,
        @Size(max = 300, message = "El enlace admite hasta 300 caracteres")
        @Pattern(regexp = "^(/(?!/)[^\\s]*|https://[^\\s]+)$",
                message = "Usa una ruta de la tienda (/productos) o una URL https") String heroCtaEnlace,
        @NotNull(message = "Envía la lista de beneficios (puede ir vacía)")
        @Size(max = 3, message = "Puedes tener hasta 3 beneficios") List<@Valid @NotNull BeneficioDto> beneficios,
        @Email(message = "Correo inválido")
        @Size(max = 150, message = "El correo admite hasta 150 caracteres") String email,
        @Size(max = 15, message = "El teléfono admite hasta 15 caracteres") String telefono,
        @Pattern(regexp = "^9[0-9]{8}$", message = "El WhatsApp debe tener 9 dígitos y empezar con 9") String whatsapp,
        @Size(max = 300, message = "La URL admite hasta 300 caracteres")
        @Pattern(regexp = "^https://.*", message = "La URL debe empezar con https://") String instagramUrl,
        @Size(max = 300, message = "La URL admite hasta 300 caracteres")
        @Pattern(regexp = "^https://.*", message = "La URL debe empezar con https://") String facebookUrl,
        @Size(max = 300, message = "La URL admite hasta 300 caracteres")
        @Pattern(regexp = "^https://.*", message = "La URL debe empezar con https://") String tiktokUrl,
        @Size(max = 120, message = "El horario admite hasta 120 caracteres") String horario,
        @Valid TiendaFisicaRequest tiendaFisica,
        @NotNull(message = "Falta la versión de la configuración") Integer version) {
}
