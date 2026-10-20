package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.GuiaTallas;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaTallasRepository extends JpaRepository<GuiaTallas, Long> {

    Optional<GuiaTallas> findByCategoriaId(Long categoriaId);
}
