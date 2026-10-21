package com.fitstore.notification.dto;

import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import java.time.Instant;
import java.util.UUID;

/** Fila del registro de envíos (P-7-01). */
public record EnvioResumenDto(UUID id, CatalogoPlantillas plantilla, String destinatario, String asunto,
        String referencia, EstadoEnvio estado, int intentos, boolean conAdjunto, Instant creadoEn, Instant enviadoEn) {
}
