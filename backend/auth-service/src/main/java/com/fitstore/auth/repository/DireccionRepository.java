package com.fitstore.auth.repository;

import com.fitstore.auth.model.Direccion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByClienteId(UUID clienteId);

    long countByClienteId(UUID clienteId);

    Optional<Direccion> findByClienteIdAndPredeterminadaTrue(UUID clienteId);
}
