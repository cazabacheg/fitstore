package com.fitstore.settings.reclamaciones.dto;

import com.fitstore.settings.reclamaciones.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del consumidor de la Hoja de Reclamación. El DNI lleva 8 dígitos y el carné de extranjería de 8 a 12
 * caracteres; si es menor de edad, el nombre del padre, madre o apoderado es obligatorio (CA-3-24).
 */
public record ConsumidorRequest(
        @NotBlank(message = "Indica tus nombres") @Size(min = 2, max = 80, message = "Los nombres admiten de 2 a 80 caracteres") String nombres,
        @NotBlank(message = "Indica tus apellidos") @Size(min = 2, max = 80, message = "Los apellidos admiten de 2 a 80 caracteres") String apellidos,
        @NotNull(message = "Elige el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Indica tu número de documento")
        @Pattern(regexp = "^[0-9A-Za-z]{8,12}$", message = "El documento debe tener de 8 a 12 caracteres") String numeroDocumento,
        @NotBlank(message = "Indica tu domicilio") @Size(max = 200, message = "El domicilio admite hasta 200 caracteres") String domicilio,
        @NotBlank(message = "Indica tu teléfono") @Size(max = 15, message = "El teléfono admite hasta 15 caracteres") String telefono,
        @NotBlank(message = "Indica tu correo") @Email(message = "Correo inválido")
        @Size(max = 150, message = "El correo admite hasta 150 caracteres") String email,
        @NotNull(message = "Indica si eres menor de edad") Boolean esMenor,
        @Size(max = 160, message = "El nombre admite hasta 160 caracteres") String apoderadoNombre) {
}
