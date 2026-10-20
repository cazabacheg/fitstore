package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.Agencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgenciaRepository extends JpaRepository<Agencia, Long> {

    /** Agencias del select del checkout, en su orden. */
    List<Agencia> findByActivaTrueOrderByOrdenAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
