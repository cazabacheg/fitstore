package com.fitstore.settings.configuracion.repository;

import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.TipoImagen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenTiendaRepository extends JpaRepository<ImagenTienda, Long> {

    List<ImagenTienda> findByTipoOrderByOrdenAsc(TipoImagen tipo);

    long countByTipo(TipoImagen tipo);

    boolean existsByClave(String clave);
}
