package com.fitstore.payment.repository;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Transaccion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {

    List<Transaccion> findByIntencionIdOrderByCreadoEnAsc(UUID intencionId);

    Optional<Transaccion> findByIntencionIdAndIdempotencyKey(UUID intencionId, String idempotencyKey);

    /** Las transacciones que el verificador debe consultar a la pasarela (por ejemplo, {@code PROCESANDO} con más de 2 min). */
    List<Transaccion> findByEstadoAndActualizadoEnBefore(EstadoTransaccion estado, Instant antesDe);
}
