package com.fitstore.notification.repository;

import com.fitstore.notification.model.PedidoResumen;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoResumenRepository extends JpaRepository<PedidoResumen, UUID> {

    /** Limpieza de retención (180 días): borra los resúmenes creados antes del límite y devuelve cuántos. */
    @Modifying
    @Query("DELETE FROM PedidoResumen p WHERE p.creadoEn < :limite")
    int purgarAnterioresA(@Param("limite") Instant limite);
}
