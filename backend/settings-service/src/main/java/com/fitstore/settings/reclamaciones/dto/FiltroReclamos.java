package com.fitstore.settings.reclamaciones.dto;

import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import java.time.LocalDate;

/** Filtros de la bandeja; todos son opcionales. {@code q} busca por número, nombre o documento. */
public record FiltroReclamos(EstadoReclamo estado, TipoReclamo tipo, boolean porVencer, boolean vencidos, String q,
        LocalDate desde, LocalDate hasta) {
}
