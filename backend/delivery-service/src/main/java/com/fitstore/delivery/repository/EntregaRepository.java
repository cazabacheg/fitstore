package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Entrega;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Los filtros del listado (estado, modalidad, fecha, repartidor, código de pedido) se componen con {@code Specification}. */
public interface EntregaRepository extends JpaRepository<Entrega, UUID>, JpaSpecificationExecutor<Entrega> {

    Optional<Entrega> findByPedidoId(UUID pedidoId);

    List<Entrega> findByFechaProgramadaAndRepartidorId(LocalDate fechaProgramada, Long repartidorId);

    boolean existsByRepartidorId(Long repartidorId);

    boolean existsByAgenciaId(Long agenciaId);
}
