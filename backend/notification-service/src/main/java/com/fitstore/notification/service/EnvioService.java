package com.fitstore.notification.service;

import com.fitstore.notification.dto.EnvioDto;
import com.fitstore.notification.dto.PaginaEnviosDto;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import java.time.LocalDate;
import java.util.UUID;

/** Registro de envíos del backoffice: listar con filtros, ver el detalle y reenviar (HU-7-05). */
public interface EnvioService {

    PaginaEnviosDto listar(EstadoEnvio estado, CatalogoPlantillas plantilla, String destinatario, String referencia,
            LocalDate desde, LocalDate hasta, int pagina, int tamano);

    EnvioDto obtener(UUID id);

    /** Vuelve un envío FALLIDO o ENVIADO a PENDIENTE; el despachador lo envía con la marca y los textos actuales (N5). */
    EnvioDto reenviar(UUID id);
}
