package com.fitstore.auth.repository;

import com.fitstore.auth.model.Direccion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByClienteId(UUID clienteId);

    /** La predeterminada primero (A1). */
    List<Direccion> findByClienteIdOrderByPredeterminadaDescIdAsc(UUID clienteId);

    /** Propiedad: solo si la dirección es de ese cliente. */
    Optional<Direccion> findByIdAndClienteId(Long id, UUID clienteId);

    /** A2: la creada más recientemente (el id desempata). */
    Optional<Direccion> findFirstByClienteIdOrderByCreadoEnDescIdDesc(UUID clienteId);

    long countByClienteId(UUID clienteId);

    Optional<Direccion> findByClienteIdAndPredeterminadaTrue(UUID clienteId);
}
