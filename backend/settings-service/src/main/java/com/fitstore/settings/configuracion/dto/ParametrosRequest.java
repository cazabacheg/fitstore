package com.fitstore.settings.configuracion.dto;

import com.fitstore.settings.configuracion.model.MetodoPago;
import com.fitstore.settings.configuracion.model.ModalidadEntrega;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/** Guardar los parámetros del negocio (CA-3-17). La moneda no se envía: siempre es PEN. */
public record ParametrosRequest(
        @NotEmpty(message = "Habilita al menos un medio de pago") Set<@NotNull MetodoPago> metodosPago,
        @NotEmpty(message = "Habilita al menos una modalidad de entrega") Set<@NotNull ModalidadEntrega> modalidadesEntrega,
        @NotNull(message = "Indica la expiración del pedido")
        @Min(value = 10, message = "La expiración debe ser de 10 a 120 minutos")
        @Max(value = 120, message = "La expiración debe ser de 10 a 120 minutos") Integer expiracionPedidoMinutos,
        @NotNull(message = "Indica los días para reembolso")
        @Min(value = 0, message = "Los días para reembolso van de 0 a 365 (0 = sin límite)")
        @Max(value = 365, message = "Los días para reembolso van de 0 a 365 (0 = sin límite)") Integer diasReembolsoPostventa,
        @NotEmpty(message = "Indica al menos un correo para alertas")
        @Size(max = 5, message = "Puedes indicar hasta 5 correos para alertas")
        List<@NotBlank @Email(message = "Correo inválido") String> correosAlertas,
        @NotNull(message = "Falta la versión de la configuración") Integer version) {
}
