package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Imagen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    List<Imagen> findByProductoIdOrderByOrdenAsc(Long productoId);
}
