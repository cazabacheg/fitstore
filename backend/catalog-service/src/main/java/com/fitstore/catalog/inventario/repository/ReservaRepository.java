package com.fitstore.catalog.inventario.repository;

import com.fitstore.catalog.inventario.model.Reserva;
import com.fitstore.catalog.inventario.model.EstadoReserva;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    /** Reservas que vencieron: las toma el liberador de respaldo (micro 4.7). */
    List<Reserva> findByEstadoAndExpiraEnBefore(EstadoReserva estado, Instant limite);
}
