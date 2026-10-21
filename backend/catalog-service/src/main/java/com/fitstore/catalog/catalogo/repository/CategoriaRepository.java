package com.fitstore.catalog.catalogo.repository;

import java.util.Optional;
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

    Optional<Categoria> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Categoria> findAllByOrderByOrdenAscNombreAsc();

    boolean existsByPadreId(Long padreId);

    boolean existsByPadreIdAndNombreIgnoreCase(Long padreId, String nombre);

    boolean existsByPadreIdIsNullAndNombreIgnoreCase(String nombre);

    /** Cuántos productos tiene asignados cada categoría (asignación directa): pares {@code [categoriaId, cantidad]}. */
    @Query("select c.id, count(p) from Producto p join p.categorias c group by c.id")
    List<Object[]> contarProductosPorCategoria();

    @Query("select count(p) from Producto p join p.categorias c where c.id = :id")
    long contarProductos(@Param("id") Long id);
}
