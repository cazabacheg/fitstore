package com.fitstore.catalog.inventario.repository;

import com.fitstore.catalog.inventario.model.Reserva;
import com.fitstore.catalog.inventario.model.EstadoReserva;
import java.time.Instant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    /** Reservas que vencieron: las toma el liberador de respaldo (micro 4.7). */
    List<Reserva> findByEstadoAndExpiraEnBefore(EstadoReserva estado, Instant limite);

    /** Lee la reserva bloqueándola: dos eventos del mismo pedido (o el liberador) no la cambian a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.pedidoId = :pedidoId")
    Optional<Reserva> buscarConBloqueo(@Param("pedidoId") UUID pedidoId);
}
