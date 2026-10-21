package com.fitstore.delivery.repository;

import com.fitstore.delivery.model.EntregaEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaEventoRepository extends JpaRepository<EntregaEvento, Long> {

    List<EntregaEvento> findByEntregaIdOrderByCreadoEnAsc(UUID entregaId);

    /** El seguimiento en orden; el id desempata los pasos que ocurren en el mismo instante. */
    List<EntregaEvento> findByEntregaIdOrderByCreadoEnAscIdAsc(UUID entregaId);
}
