package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.agencia;
import static com.fitstore.delivery.pruebas.Datos.entrega;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class AgenciaRepositoryTest {

    @Autowired
    AgenciaRepository repositorio;

    @Autowired
    EntregaRepository entregas;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void sinSemilla() {
        // V2 siembra agencias: cada caso parte de una tabla vacía (la transacción se revierte al terminar)
        entregas.deleteAllInBatch();
        repositorio.deleteAllInBatch();
    }

    @Test
    @DisplayName("CA-0a-06 inserta una agencia activa con su plantilla de seguimiento")
    void inserta() {
        Agencia guardada = repositorio.saveAndFlush(agencia("Shalom", 1));
        em.clear();

        Agencia releida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(releida.getNombre()).isEqualTo("Shalom");
        assertThat(releida.getUrlSeguimiento()).contains("{codigo}");
        assertThat(releida.isActiva()).isTrue();
        assertThat(releida.getOrden()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el nombre, el orden y el estado de una agencia")
    void actualiza() {
        Long id = repositorio.saveAndFlush(agencia("Shalom", 1)).getId();
        em.clear();

        Agencia agencia = repositorio.findById(id).orElseThrow();
        agencia.setNombre("Shalom Empresarial");
        agencia.setOrden(3);
        agencia.setActiva(false);
        repositorio.saveAndFlush(agencia);
        em.clear();

        Agencia releida = repositorio.findById(id).orElseThrow();
        assertThat(releida.getNombre()).isEqualTo("Shalom Empresarial");
        assertThat(releida.getOrden()).isEqualTo(3);
        assertThat(releida.isActiva()).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 elimina físicamente una agencia sin entregas")
    void eliminaSinEntregas() {
        Long id = repositorio.saveAndFlush(agencia("Shalom", 1)).getId();

        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(repositorio.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 no elimina una agencia que tiene entregas")
    void noEliminaConEntregas() {
        Agencia agencia = repositorio.saveAndFlush(agencia("Shalom", 1));
        Entrega entrega = entrega(Modalidad.AGENCIA);
        entrega.setAgencia(agencia);
        entregas.saveAndFlush(entrega);

        assertThat(entregas.existsByAgenciaId(agencia.getId())).isTrue();
        assertThatThrownBy(() -> {
            repositorio.deleteById(agencia.getId());
            repositorio.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 lista solo las agencias activas, en su orden")
    void listaActivasEnOrden() {
        repositorio.save(agencia("Olva Courier", 2));
        repositorio.save(agencia("Shalom", 1));
        Agencia inactiva = agencia("Marvisur", 3);
        inactiva.setActiva(false);
        repositorio.save(inactiva);
        repositorio.flush();

        assertThat(repositorio.findByActivaTrueOrderByOrdenAsc()).extracting(Agencia::getNombre)
                .containsExactly("Shalom", "Olva Courier");
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un nombre repetido sin distinguir mayúsculas")
    void rechazaNombreRepetido() {
        repositorio.saveAndFlush(agencia("Shalom", 1));

        assertThat(repositorio.existsByNombreIgnoreCase("SHALOM")).isTrue();
        assertThatThrownBy(() -> repositorio.saveAndFlush(agencia("SHALOM", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
