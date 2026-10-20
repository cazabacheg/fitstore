package com.fitstore.settings.configuracion.repository;

import com.fitstore.settings.configuracion.model.Politica;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaRepository extends JpaRepository<Politica, Long> {

    /** La versión vigente es la de mayor número. */
    Optional<Politica> findFirstByTipoOrderByVersionDesc(TipoPolitica tipo);

    List<Politica> findByTipoOrderByVersionDesc(TipoPolitica tipo);

    Optional<Politica> findByTipoAndVersion(TipoPolitica tipo, int version);
}
