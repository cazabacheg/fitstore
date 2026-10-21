package com.fitstore.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registro de un cliente (CA-1-01 a 03). Las versiones son las de las políticas que mostró la tienda. */
public record RegistroRequest(
        @NotBlank(message = Validaciones.MENSAJE_EMAIL) @Email(message = Validaciones.MENSAJE_EMAIL)
        @Size(max = 254, message = Validaciones.MENSAJE_EMAIL) String email,
        @NotNull(message = Validaciones.MENSAJE_PASSWORD)
        @Pattern(regexp = Validaciones.PASSWORD, message = Validaciones.MENSAJE_PASSWORD) String password,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String nombres,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String apellidos,
        @NotNull(message = Validaciones.MENSAJE_DNI) @Pattern(regexp = Validaciones.DNI, message = Validaciones.MENSAJE_DNI) String dni,
        @NotNull(message = Validaciones.MENSAJE_CELULAR)
        @Pattern(regexp = Validaciones.CELULAR, message = Validaciones.MENSAJE_CELULAR) String telefono,
        @AssertTrue(message = "Debes aceptar los términos y condiciones") @Schema(allowableValues = "true") boolean aceptaTerminos,
        @AssertTrue(message = "Debes aceptar la política de privacidad") @Schema(allowableValues = "true") boolean aceptaPrivacidad,
        @NotNull(message = "Falta la versión de los términos") @Min(value = 1, message = "Versión inválida") Integer versionTerminos,
        @NotNull(message = "Falta la versión de la política de privacidad") @Min(value = 1, message = "Versión inválida") Integer versionPrivacidad) {
}
