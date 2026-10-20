package com.fitstore.payment.repository;

import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.Reembolso;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReembolsoRepository extends JpaRepository<Reembolso, UUID> {

    List<Reembolso> findByIntencionId(UUID intencionId);

    Optional<Reembolso> findBySolicitudId(UUID solicitudId);

    List<Reembolso> findByEstado(EstadoReembolso estado);

    /** Los PENDIENTE que el procesador debe intentar ahora: sin fecha de reintento o con la fecha ya cumplida. */
    @Query("""
            select r from Reembolso r
            where r.estado = com.fitstore.payment.model.EstadoReembolso.PENDIENTE
              and (r.proximoIntentoEn is null or r.proximoIntentoEn <= :ahora)
            order by r.creadoEn""")
    List<Reembolso> pendientesParaProcesar(@Param("ahora") Instant ahora);
}
