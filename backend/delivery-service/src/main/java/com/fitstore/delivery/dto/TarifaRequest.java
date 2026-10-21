package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Modalidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Alta o edición de una tarifa. El código de ubigeo define el nivel: 2, 4 o 6 dígitos (D1). */
public record TarifaRequest(
        @NotNull Modalidad modalidad,
        @NotNull @Pattern(regexp = "^([0-9]{2}|[0-9]{4}|[0-9]{6})$", message = "Elige un departamento, una provincia o un distrito")
        String ubigeo,
        @NotNull @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo admite hasta 2 decimales") BigDecimal costo,
        @NotNull @Min(0) @Max(30) Integer plazoDiasMin,
        @NotNull @Min(0) @Max(30) Integer plazoDiasMax,
        Boolean activa) {
}
