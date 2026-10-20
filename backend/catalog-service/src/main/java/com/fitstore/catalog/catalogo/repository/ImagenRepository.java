package com.fitstore.catalog.catalogo.repository;

import java.util.Collection;
import com.fitstore.catalog.catalogo.model.Imagen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    List<Imagen> findByProductoIdOrderByOrdenAsc(Long productoId);

    List<Imagen> findByProductoIdInOrderByOrdenAsc(Collection<Long> productoIds);

    List<Imagen> findByProductoIdInAndPrincipalTrue(Collection<Long> productoIds);

    long countByProductoId(Long productoId);
}
