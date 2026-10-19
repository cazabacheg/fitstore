package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.configuracion.model.Politica;
import com.fitstore.settings.configuracion.model.TipoPolitica;
import com.fitstore.settings.configuracion.repository.PoliticaRepository;
import com.fitstore.settings.pruebas.Datos;
import com.fitstore.settings.pruebas.PruebaDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PoliticaRepositoryTest extends PruebaDatos {

    @Autowired PoliticaRepository repositorio;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        // Cada prueba arma su propio historial (se revierte al terminar)
        jdbc.update("DELETE FROM politica");
    }

    @Test
    @DisplayName("CA-0a-06 inserta una nueva versión de una política")
    void insertaNuevaVersion() {
        repositorio.saveAndFlush(Datos.politica(TipoPolitica.TERMINOS, 1));
        Politica v2 = repositorio.saveAndFlush(Datos.politica(TipoPolitica.TERMINOS, 2));

        assertThat(repositorio.findById(v2.getId())).get()
                .extracting(Politica::getVersion, Politica::getContenido)
                .containsExactly(2, "# Política v2");
    }

    @Test
    @DisplayName("CA-0a-06 la versión vigente es la de mayor número")
    void vigenteEsLaMayorVersion() {
        repositorio.save(Datos.politica(TipoPolitica.PRIVACIDAD, 1));
        repositorio.save(Datos.politica(TipoPolitica.PRIVACIDAD, 3));
        repositorio.save(Datos.politica(TipoPolitica.PRIVACIDAD, 2));
        repositorio.save(Datos.politica(TipoPolitica.ENVIOS, 7));
        repositorio.flush();

        assertThat(repositorio.findFirstByTipoOrderByVersionDesc(TipoPolitica.PRIVACIDAD)).get()
                .extracting(Politica::getVersion).isEqualTo(3);
        assertThat(repositorio.findFirstByTipoOrderByVersionDesc(TipoPolitica.CAMBIOS_DEVOLUCIONES)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 lista el historial de versiones de la más reciente a la más antigua")
    void historial() {
        repositorio.save(Datos.politica(TipoPolitica.TERMINOS, 1));
        repositorio.save(Datos.politica(TipoPolitica.TERMINOS, 2));
        repositorio.save(Datos.politica(TipoPolitica.TERMINOS, 3));
        repositorio.save(Datos.politica(TipoPolitica.ENVIOS, 1));
        repositorio.flush();

        assertThat(repositorio.findByTipoOrderByVersionDesc(TipoPolitica.TERMINOS))
                .extracting(Politica::getVersion).containsExactly(3, 2, 1);
        assertThat(repositorio.findByTipoAndVersion(TipoPolitica.TERMINOS, 2)).isPresent();
        assertThat(repositorio.findByTipoAndVersion(TipoPolitica.TERMINOS, 9)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 (tipo, versión) es único")
    void tipoYVersionUnicos() {
        repositorio.saveAndFlush(Datos.politica(TipoPolitica.TERMINOS, 1));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.politica(TipoPolitica.TERMINOS, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 la misma versión puede existir en otro tipo de política")
    void mismaVersionEnOtroTipo() {
        repositorio.save(Datos.politica(TipoPolitica.TERMINOS, 1));
        repositorio.save(Datos.politica(TipoPolitica.ENVIOS, 1));
        repositorio.flush();

        assertThat(repositorio.count()).isEqualTo(2);
    }
}
