package com.fitstore.settings.reclamaciones.repository;

import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Búsqueda de la bandeja con filtros opcionales (fragmento de {@link ReclamoRepository}). */
public interface ReclamoBusqueda {

    /**
     * @param hoy            fecha de hoy en Lima, para "vencidos" y "por vencer"
     * @param limitePorVencer última fecha que cuenta como "por vencer" (hoy más 3 días hábiles)
     */
    Page<Reclamo> buscar(FiltroReclamos filtro, LocalDate hoy, LocalDate limitePorVencer, Pageable paginacion);
}
