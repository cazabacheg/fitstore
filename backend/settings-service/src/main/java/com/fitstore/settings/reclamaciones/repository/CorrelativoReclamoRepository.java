package com.fitstore.settings.reclamaciones.repository;

import com.fitstore.settings.reclamaciones.model.CorrelativoReclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrelativoReclamoRepository extends JpaRepository<CorrelativoReclamo, Integer> {

    /**
     * Siguiente correlativo del año, atómico y sin huecos: se pide en la misma transacción que guarda el reclamo,
     * así que si esta se revierte el número no se consume. El bloqueo de la fila serializa a los concurrentes.
     */
    @Query(value = """
            INSERT INTO correlativo_reclamo (anio, ultimo) VALUES (:anio, 1)
            ON CONFLICT (anio) DO UPDATE SET ultimo = correlativo_reclamo.ultimo + 1
            RETURNING ultimo
            """, nativeQuery = true)
    int siguiente(@Param("anio") int anio);
}
