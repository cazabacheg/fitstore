package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Talla;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TallaRepository extends JpaRepository<Talla, Long> {

    Optional<Talla> findByCodigo(String codigo);

    List<Talla> findAllByOrderByOrdenAsc();

    List<Talla> findByActivoTrueOrderByOrdenAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Talla> findByCodigoIgnoreCase(String codigo);
}
