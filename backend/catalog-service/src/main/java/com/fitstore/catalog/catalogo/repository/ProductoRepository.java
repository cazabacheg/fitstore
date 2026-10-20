package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCodigo(String codigo);

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByEstado(EstadoProducto estado);
}
