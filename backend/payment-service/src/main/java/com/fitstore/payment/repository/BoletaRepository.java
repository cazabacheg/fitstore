package com.fitstore.payment.repository;

import com.fitstore.payment.model.Boleta;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoletaRepository extends JpaRepository<Boleta, UUID> {

    Optional<Boleta> findByIntencionId(UUID intencionId);

    Optional<Boleta> findByNumero(String numero);

    @Query("select b from Boleta b, IntencionPago i where b.intencionId = i.id and i.pedidoId = :pedidoId")
    Optional<Boleta> findByPedidoId(@Param("pedidoId") UUID pedidoId);
}
