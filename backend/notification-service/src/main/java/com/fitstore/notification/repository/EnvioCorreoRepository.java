package com.fitstore.notification.repository;

import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnvioCorreoRepository extends JpaRepository<EnvioCorreo, UUID> {

    /**
     * Lote de pendientes cuyo próximo intento ya venció, del más antiguo al más nuevo. {@code FOR UPDATE SKIP LOCKED}
     * hace seguro el despacho aunque algún día haya dos instancias (micro 10.2); debe llamarse dentro de una transacción.
     */
    @Query(value = """
            SELECT * FROM envio_correo
            WHERE estado = 'PENDIENTE' AND proximo_intento_en <= :ahora
            ORDER BY proximo_intento_en
            LIMIT :lote
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EnvioCorreo> tomarPendientes(@Param("ahora") Instant ahora, @Param("lote") int lote);

    /** Registro de envíos del backoffice (micro 4.5); cada filtro es opcional y el más reciente va primero. */
    @Query("""
            SELECT e FROM EnvioCorreo e
            WHERE (:estado IS NULL OR e.estado = :estado)
              AND (:plantilla IS NULL OR e.plantilla = :plantilla)
              AND (:destinatario IS NULL OR LOWER(e.destinatario) LIKE LOWER(CONCAT('%', CAST(:destinatario AS string), '%')))
              AND (:referencia IS NULL OR e.referencia = :referencia)
              AND (CAST(:desde AS instant) IS NULL OR e.creadoEn >= :desde)
              AND (CAST(:hasta AS instant) IS NULL OR e.creadoEn <= :hasta)
            ORDER BY e.creadoEn DESC
            """)
    Page<EnvioCorreo> filtrar(@Param("estado") EstadoEnvio estado, @Param("plantilla") String plantilla,
            @Param("destinatario") String destinatario, @Param("referencia") String referencia,
            @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable paginacion);

    /** Limpieza de retención (180 días): borra los envíos creados antes del límite y devuelve cuántos. */
    @Modifying
    @Query("DELETE FROM EnvioCorreo e WHERE e.creadoEn < :limite")
    int purgarAnterioresA(@Param("limite") Instant limite);

    /** Número de envíos por estado con los mismos filtros del registro, salvo el estado (chips del listado). */
    @Query("""
            SELECT e.estado, COUNT(e) FROM EnvioCorreo e
            WHERE (:plantilla IS NULL OR e.plantilla = :plantilla)
              AND (:destinatario IS NULL OR LOWER(e.destinatario) LIKE LOWER(CONCAT('%', CAST(:destinatario AS string), '%')))
              AND (:referencia IS NULL OR e.referencia = :referencia)
              AND (CAST(:desde AS instant) IS NULL OR e.creadoEn >= :desde)
              AND (CAST(:hasta AS instant) IS NULL OR e.creadoEn <= :hasta)
            GROUP BY e.estado
            """)
    List<Object[]> contarPorEstado(@Param("plantilla") String plantilla, @Param("destinatario") String destinatario,
            @Param("referencia") String referencia, @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
