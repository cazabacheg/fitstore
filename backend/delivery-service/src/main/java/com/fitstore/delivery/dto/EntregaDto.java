package com.fitstore.delivery.dto;

import com.fitstore.delivery.model.AccionEntrega;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EntregaEvento;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.service.dominio.MaquinaEstadosEntrega;
import com.fitstore.delivery.service.dominio.UrlRastreo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalle de una entrega: el resumen más destinatario, dirección, seguimiento, acciones permitidas (E5) y versión (E6). */
public record EntregaDto(
        UUID id, UUID pedidoId, String codigoPedido, Modalidad modalidad, EstadoEntrega estado,
        String destinatarioNombre, @Schema(nullable = true) String distrito,
        @Schema(nullable = true) LocalDate fechaProgramada, @Schema(nullable = true) String repartidorNombre,
        @Schema(nullable = true) String agenciaNombre, int intento,
        @Schema(nullable = true) AccionEntrega siguienteAccion,
        DestinatarioEntregaDto destinatario, DireccionEntregaDto direccion, String resumenItems, int bultos,
        RepartidorAsignadoDto repartidor, @Schema(nullable = true) Long agenciaId,
        @Schema(nullable = true) String codigoSeguimiento, @Schema(nullable = true) String claveRecojo,
        @Schema(nullable = true) String urlSeguimiento, List<EventoSeguimientoDto> eventos,
        List<AccionEntrega> accionesPermitidas, int version) {

    public static EntregaDto de(Entrega e, List<EntregaEvento> seguimiento) {
        EntregaResumenDto r = EntregaResumenDto.de(e);
        return new EntregaDto(r.id(), r.pedidoId(), r.codigoPedido(), r.modalidad(), r.estado(), r.destinatarioNombre(),
                r.distrito(), r.fechaProgramada(), r.repartidorNombre(), r.agenciaNombre(), r.intento(),
                r.siguienteAccion(), DestinatarioEntregaDto.de(e.getDestinatario()),
                DireccionEntregaDto.de(e.getDireccion()), e.getResumenItems(), e.getBultos(),
                RepartidorAsignadoDto.de(e.getRepartidor()), e.getAgencia() == null ? null : e.getAgencia().getId(),
                e.getCodigoSeguimiento(), e.getClaveRecojo(), UrlRastreo.armar(e.getAgencia(), e.getCodigoSeguimiento()),
                seguimiento.stream().map(EventoSeguimientoDto::de).toList(),
                MaquinaEstadosEntrega.accionesPermitidas(e.getModalidad(), e.getEstado()),
                e.getVersion() == null ? 0 : e.getVersion().intValue());
    }
}
