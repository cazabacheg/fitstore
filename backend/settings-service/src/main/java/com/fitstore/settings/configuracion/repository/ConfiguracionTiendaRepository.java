package com.fitstore.settings.configuracion.repository;

import com.fitstore.settings.configuracion.model.ConfiguracionTienda;
import org.springframework.data.jpa.repository.JpaRepository;

/** La configuración es una sola fila (id = 1). */
public interface ConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, Integer> {
}
