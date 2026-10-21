package com.fitstore.payment.dto;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Cobro: solo el proveedor, el medio y el token de un solo uso; el monto sale siempre de la intención (RF-PAY-00). */
public record CargoRequest(
        @NotNull Proveedor proveedor,
        @NotNull Medio medio,
        @NotNull @Size(min = 4, max = 200) String token,
        @Schema(nullable = true) @Email String email,
        @Schema(nullable = true) Map<String, Object> datosAdicionales) {

    /** No se imprime el token en ningún log. */
    @Override
    public String toString() {
        return "CargoRequest[proveedor=" + proveedor + ", medio=" + medio + "]";
    }
}
