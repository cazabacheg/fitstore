package com.fitstore.notification.dto;

import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Detalle de un envío: la fila del registro más cada intento, el último error y el próximo intento. */
public record EnvioDto(UUID id, CatalogoPlantillas plantilla, String destinatario, String asunto, String referencia,
        EstadoEnvio estado, int intentos, boolean conAdjunto, Instant creadoEn, Instant enviadoEn,
        List<IntentoDto> historialIntentos, String ultimoError, Instant proximoIntentoEn) {
}
