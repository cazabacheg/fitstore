package com.fitstore.auth.repository;

import com.fitstore.auth.model.Consentimiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    List<Consentimiento> findByClienteId(UUID clienteId);
}
