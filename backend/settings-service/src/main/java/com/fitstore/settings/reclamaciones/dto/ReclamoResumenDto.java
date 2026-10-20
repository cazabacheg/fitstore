package com.fitstore.settings.reclamaciones.dto;

import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import java.time.Instant;
import java.time.LocalDate;

/** Fila de la bandeja de reclamos; "por vencer" y "vencido" se calculan con la fecha de hoy. */
public record ReclamoResumenDto(Long id, String numero, TipoReclamo tipo, EstadoReclamo estado,
        String consumidorNombre, Instant creadoEn, LocalDate fechaLimite, boolean porVencer, boolean vencido) {
}
