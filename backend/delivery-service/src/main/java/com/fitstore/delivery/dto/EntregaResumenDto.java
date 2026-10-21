package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.AccionEntrega;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.service.dominio.MaquinaEstadosEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/** Una fila del listado de entregas, con el siguiente paso para el botón de la fila (E1). */
public record EntregaResumenDto(UUID id, UUID pedidoId, String codigoPedido, Modalidad modalidad, EstadoEntrega estado,
        String destinatarioNombre, @Schema(nullable = true) String distrito,
        @Schema(nullable = true) LocalDate fechaProgramada, @Schema(nullable = true) String repartidorNombre,
        @Schema(nullable = true) String agenciaNombre, int intento,
        @Schema(nullable = true, description = "El siguiente paso para el botón de la fila (E1); null en estados finales.")
        AccionEntrega siguienteAccion) {

    public static EntregaResumenDto de(Entrega e) {
        return new EntregaResumenDto(e.getId(), e.getPedidoId(), e.getCodigoPedido(), e.getModalidad(), e.getEstado(),
                e.getDestinatario().getNombres() + " " + e.getDestinatario().getApellidos(),
                e.getDireccion().getDistrito(), e.getFechaProgramada(),
                e.getRepartidor() == null ? null : e.getRepartidor().getNombres(),
                e.getAgencia() == null ? null : e.getAgencia().getNombre(), e.getIntentos(),
                MaquinaEstadosEntrega.siguienteAccion(e.getModalidad(), e.getEstado()));
    }
}
