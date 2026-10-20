package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Variante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarianteRepository extends JpaRepository<Variante, Long> {

    List<Variante> findByProductoIdOrderByIdAsc(Long productoId);

    Optional<Variante> findBySku(String sku);
}
