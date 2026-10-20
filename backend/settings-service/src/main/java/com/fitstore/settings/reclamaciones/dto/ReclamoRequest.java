package com.fitstore.settings.reclamaciones.dto;

import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Hoja de Reclamación virtual: reclamo (producto o servicio) o queja (atención). */
public record ReclamoRequest(
        @NotNull(message = "Elige si es un reclamo o una queja") TipoReclamo tipo,
        @NotNull(message = "Faltan tus datos") @Valid ConsumidorRequest consumidor,
        @NotNull(message = "Falta el bien contratado") @Valid BienContratadoRequest bien,
        @NotBlank(message = "Cuéntanos qué pasó")
        @Size(min = 10, max = 3000, message = "El detalle admite de 10 a 3000 caracteres") String detalle,
        @NotBlank(message = "Indica qué solicitas")
        @Size(min = 5, max = 1000, message = "El pedido admite de 5 a 1000 caracteres") String pedidoConsumidor) {
}
