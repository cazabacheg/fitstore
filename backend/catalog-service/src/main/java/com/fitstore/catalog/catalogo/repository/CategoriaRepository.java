package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByPadreIdIsNullOrderByOrdenAscNombreAsc();

    List<Categoria> findByPadreIdOrderByOrdenAscNombreAsc(Long padreId);

    /** La categoría y sus hijas: al listar un padre se incluyen los productos de sus hijas (D5). */
    @Query("select c.id from Categoria c where c.id = :id or c.padreId = :id")
    List<Long> idsConHijas(@Param("id") Long id);
}
