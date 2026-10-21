package com.fitstore.auth.dto.request;

import com.fitstore.auth.model.RolStaff;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Invitación de un miembro del staff: se crea sin contraseña y recibe un enlace para establecerla (D4). */
public record InvitarStaffRequest(
        @NotBlank(message = Validaciones.MENSAJE_EMAIL) @Email(message = Validaciones.MENSAJE_EMAIL)
        @Size(max = 254, message = Validaciones.MENSAJE_EMAIL) String email,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String nombres,
        @NotBlank(message = Validaciones.MENSAJE_NOMBRE) @Size(min = 2, max = 80, message = Validaciones.MENSAJE_NOMBRE) String apellidos,
        @NotNull(message = "Elige un rol") RolStaff rol) {
}
