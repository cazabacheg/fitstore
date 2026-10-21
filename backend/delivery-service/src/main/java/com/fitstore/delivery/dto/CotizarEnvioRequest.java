package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.Modalidad;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Pedido de cotización. El ubigeo es el del distrito (6 dígitos); lo valida el servicio para dar el mensaje de CA-6-03. */
public record CotizarEnvioRequest(
        @NotNull Modalidad modalidad,
        @NotNull(message = "Elige el distrito de destino")
        @Schema(pattern = "^[0-9]{6}$", description = "Código de distrito INEI de 6 dígitos.") String ubigeo,
        @Schema(nullable = true, description = "Obligatoria en AGENCIA.") Long agenciaId) {
}
