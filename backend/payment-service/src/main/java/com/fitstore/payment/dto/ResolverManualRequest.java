package com.fitstore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nota obligatoria al marcar un reembolso como resuelto fuera del sistema (extensión de alcance). */
public record ResolverManualRequest(@NotBlank @Size(min = 5, max = 500) String nota) {
}
