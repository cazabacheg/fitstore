package com.fitstore.catalog.catalogo.repository;

import com.fitstore.catalog.catalogo.model.Color;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {

    Optional<Color> findByNombreIgnoreCase(String nombre);
}
