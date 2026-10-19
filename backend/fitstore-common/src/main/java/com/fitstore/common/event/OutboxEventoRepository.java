package com.fitstore.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, UUID> {

    List<OutboxEvento> findByPublicadoEnIsNullOrderByCreadoEnAsc(Pageable pagina);

    @Modifying
    @Query("update OutboxEvento e set e.publicadoEn = :cuando where e.id = :id and e.publicadoEn is null")
    int marcarPublicado(UUID id, Instant cuando);
}
