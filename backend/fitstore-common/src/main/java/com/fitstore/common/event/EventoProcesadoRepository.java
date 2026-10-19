package com.fitstore.common.event;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, UUID> {
}
