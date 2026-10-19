package com.fitstore.settings.ubigeo.repository;

import com.fitstore.settings.ubigeo.model.Distrito;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistritoRepository extends JpaRepository<Distrito, String> {

    List<Distrito> findByProvinciaCodigoOrderByNombreAsc(String provinciaCodigo);

    /** Búsqueda por nombre sin tildes ni mayúsculas (usa el índice trigram sobre {@code f_unaccent(nombre)}). */
    @Query(value = """
            SELECT d.* FROM distrito d
            WHERE f_unaccent(lower(d.nombre)) LIKE '%' || f_unaccent(lower(:texto)) || '%'
            ORDER BY d.nombre, d.codigo
            """, nativeQuery = true)
    List<Distrito> buscarPorNombre(@Param("texto") String texto);
}
