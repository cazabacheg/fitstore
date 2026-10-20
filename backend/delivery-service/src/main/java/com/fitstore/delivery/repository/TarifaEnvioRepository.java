package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.TarifaEnvio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarifaEnvioRepository extends JpaRepository<TarifaEnvio, Long> {

    List<TarifaEnvio> findByModalidad(Modalidad modalidad);

    /**
     * Búsqueda jerárquica en una sola sentencia (micro 4.2 y 10.2): la tarifa activa más específica entre el distrito
     * ({@code d6}), su provincia ({@code p4}) y su departamento ({@code d2}); el código más largo gana.
     */
    @Query(value = """
            SELECT * FROM tarifa_envio
            WHERE modalidad = :#{#modalidad.name()} AND activa AND ubigeo IN (:d6, :p4, :d2)
            ORDER BY length(ubigeo) DESC
            LIMIT 1""", nativeQuery = true)
    Optional<TarifaEnvio> masEspecifica(@Param("modalidad") Modalidad modalidad, @Param("d6") String d6,
            @Param("p4") String p4, @Param("d2") String d2);
}
