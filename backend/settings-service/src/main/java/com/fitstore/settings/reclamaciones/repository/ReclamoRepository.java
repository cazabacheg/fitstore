package com.fitstore.settings.reclamaciones.repository;

import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Los reclamos nunca se eliminan (D8): por eso extiende {@link Repository} y no {@code JpaRepository};
 * solo expone lo necesario para registrar, consultar y actualizar.
 */
public interface ReclamoRepository extends Repository<Reclamo, Long>, ReclamoBusqueda {

    Reclamo save(Reclamo reclamo);

    Reclamo saveAndFlush(Reclamo reclamo);

    <S extends Reclamo> List<S> saveAll(Iterable<S> reclamos);

    void flush();

    Optional<Reclamo> findById(Long id);

    /** Bloquea la fila hasta el fin de la transacción: dos admins no pueden responder el mismo reclamo a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reclamo r WHERE r.id = :id")
    Optional<Reclamo> findByIdParaActualizar(@Param("id") Long id);

    Optional<Reclamo> findByNumero(String numero);

    long count();

    List<Reclamo> findByEstadoOrderByCreadoEnDesc(EstadoReclamo estado);

    List<Reclamo> findByTipoOrderByCreadoEnDesc(TipoReclamo tipo);

    List<Reclamo> findByCreadoEnBetweenOrderByCreadoEnDesc(Instant desde, Instant hasta);

    /** Sin responder y con la fecha límite ya pasada. */
    @Query("SELECT r FROM Reclamo r WHERE r.estado <> 'RESPONDIDO' AND r.fechaLimite < :hoy ORDER BY r.fechaLimite, r.id")
    List<Reclamo> findVencidos(@Param("hoy") LocalDate hoy);

    /** Sin responder y con la fecha límite entre hoy y el límite dado (ambos incluidos). */
    @Query("""
            SELECT r FROM Reclamo r
            WHERE r.estado <> 'RESPONDIDO' AND r.fechaLimite BETWEEN :hoy AND :limite
            ORDER BY r.fechaLimite, r.id
            """)
    List<Reclamo> findPorVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    /** Busca por número, nombre completo o documento; el patrón va en minúsculas y con comodines ({@code %texto%}). */
    @Query("""
            SELECT r FROM Reclamo r
            WHERE lower(r.numero) LIKE :patron
               OR lower(concat(r.consumidor.nombres, ' ', r.consumidor.apellidos)) LIKE :patron
               OR lower(r.consumidor.numeroDocumento) LIKE :patron
            ORDER BY r.creadoEn DESC, r.id DESC
            """)
    List<Reclamo> buscarPorTexto(@Param("patron") String patron);
}
