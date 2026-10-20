package com.fitstore.catalog.catalogo.repository;

import java.util.Collection;
import com.fitstore.catalog.catalogo.model.Variante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarianteRepository extends JpaRepository<Variante, Long> {

    List<Variante> findByProductoIdOrderByIdAsc(Long productoId);

    Optional<Variante> findBySku(String sku);

    List<Variante> findByProductoIdIn(Collection<Long> productoIds);

    List<Variante> findByProductoIdInAndActivoTrue(Collection<Long> productoIds);

    boolean existsByTallaId(Long tallaId);

    boolean existsByColorId(Long colorId);
}
