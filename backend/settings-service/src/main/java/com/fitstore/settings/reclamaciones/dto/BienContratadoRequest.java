package com.fitstore.settings.reclamaciones.dto;

import com.fitstore.settings.reclamaciones.model.TipoBien;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Producto o servicio reclamado; el monto y el código de pedido ({@code FS-000118}) son opcionales. */
public record BienContratadoRequest(
        @NotNull(message = "Elige si es un producto o un servicio") TipoBien tipo,
        @NotBlank(message = "Describe el bien contratado") @Size(max = 300, message = "La descripción admite hasta 300 caracteres") String descripcion,
        @DecimalMin(value = "0", message = "El monto no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal montoReclamado,
        @Pattern(regexp = "^FS-[0-9]{6}$", message = "El código de pedido tiene el formato FS-000118") String pedidoCodigo) {
}
