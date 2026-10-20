package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.configuracion.model.ImagenTienda;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.repository.ImagenTiendaRepository;
import com.fitstore.settings.pruebas.Datos;
import com.fitstore.settings.pruebas.PruebaDatos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class ImagenTiendaRepositoryTest extends PruebaDatos {

    @Autowired ImagenTiendaRepository repositorio;
    @Autowired EntityManager em;

    @Test
    @DisplayName("CA-0a-06 inserta una imagen y la lee por id")
    void inserta() {
        ImagenTienda guardada = repositorio.saveAndFlush(Datos.imagen(TipoImagen.LOGO_CLARO, 0));
        em.clear();

        ImagenTienda leida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getTipo()).isEqualTo(TipoImagen.LOGO_CLARO);
        assertThat(leida.getClave()).startsWith("tienda/logo_claro/");
        assertThat(leida.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el orden, el texto alternativo y el enlace")
    void actualiza() {
        ImagenTienda guardada = repositorio.saveAndFlush(Datos.imagen(TipoImagen.LANDING, 0));

        guardada.setOrden(3);
        guardada.setTextoAlternativo("Colección nueva");
        guardada.setEnlace("/catalogo");
        repositorio.saveAndFlush(guardada);
        em.clear();

        ImagenTienda leida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getOrden()).isEqualTo(3);
        assertThat(leida.getTextoAlternativo()).isEqualTo("Colección nueva");
        assertThat(leida.getEnlace()).isEqualTo("/catalogo");
    }

    @Test
    @DisplayName("CA-0a-06 elimina físicamente una imagen")
    void eliminaFisicamente() {
        ImagenTienda guardada = repositorio.saveAndFlush(Datos.imagen(TipoImagen.LANDING, 0));

        repositorio.deleteById(guardada.getId());
        repositorio.flush();

        assertThat(repositorio.findById(guardada.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 lista por tipo ordenado por orden")
    void listaPorTipo() {
        repositorio.save(Datos.imagen(TipoImagen.LANDING, 2));
        repositorio.save(Datos.imagen(TipoImagen.LANDING, 0));
        repositorio.save(Datos.imagen(TipoImagen.LANDING, 1));
        repositorio.save(Datos.imagen(TipoImagen.FAVICON, 0));
        repositorio.flush();

        assertThat(repositorio.findByTipoOrderByOrdenAsc(TipoImagen.LANDING))
                .extracting(ImagenTienda::getOrden).containsExactly(0, 1, 2);
        assertThat(repositorio.findByTipoOrderByOrdenAsc(TipoImagen.FAVICON)).hasSize(1);
        assertThat(repositorio.countByTipo(TipoImagen.LANDING)).isEqualTo(3);
        assertThat(repositorio.findByTipoOrderByOrdenAsc(TipoImagen.LOGO_OSCURO)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 un solo logo o favicon por tipo; la landing admite varias")
    void unSoloLogoPorTipo() {
        repositorio.saveAndFlush(Datos.imagen(TipoImagen.LOGO_OSCURO, 0));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.imagen(TipoImagen.LOGO_OSCURO, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 la landing admite varias imágenes del mismo tipo")
    void landingAdmiteVarias() {
        repositorio.save(Datos.imagen(TipoImagen.LANDING, 0));
        repositorio.save(Datos.imagen(TipoImagen.LANDING, 1));
        repositorio.flush();

        assertThat(repositorio.countByTipo(TipoImagen.LANDING)).isEqualTo(2);
    }
}
