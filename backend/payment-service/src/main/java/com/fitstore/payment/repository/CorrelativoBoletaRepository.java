package com.fitstore.payment.repository;

import com.fitstore.payment.model.CorrelativoBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrelativoBoletaRepository extends JpaRepository<CorrelativoBoleta, String> {

    /**
     * Toma el siguiente número de la serie con una sola sentencia atómica. Debe ejecutarse dentro de la transacción
     * que guarda la boleta: si esta se revierte, el número no se consume y no quedan huecos.
     */
    @Query(value = "UPDATE correlativo_boleta SET ultimo = ultimo + 1 WHERE serie = :serie RETURNING ultimo",
            nativeQuery = true)
    int siguiente(@Param("serie") String serie);
}
