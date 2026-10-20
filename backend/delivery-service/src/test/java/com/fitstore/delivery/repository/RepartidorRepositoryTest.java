package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.entrega;
import static com.fitstore.delivery.pruebas.Datos.repartidor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class RepartidorRepositoryTest {

    @Autowired
    RepartidorRepository repositorio;

    @Autowired
    EntregaRepository entregas;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 inserta un repartidor activo")
    void inserta() {
        Repartidor guardado = repositorio.saveAndFlush(repartidor("Juan Pérez"));
        em.clear();

        Repartidor releido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(releido.getNombres()).isEqualTo("Juan Pérez");
        assertThat(releido.getVehiculo()).isEqualTo(TipoVehiculo.MOTO);
        assertThat(releido.isActivo()).isTrue();
        assertThat(releido.getCreadoEn()).isNotNull();
        assertThat(releido.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza los datos y el estado de un repartidor")
    void actualiza() {
        Long id = repositorio.saveAndFlush(repartidor("Juan Pérez")).getId();
        em.clear();

        Repartidor repartidor = repositorio.findById(id).orElseThrow();
        repartidor.setTelefono("911222333");
        repartidor.setVehiculo(TipoVehiculo.BICICLETA);
        repartidor.setPlaca(null);
        repartidor.setActivo(false);
        repositorio.saveAndFlush(repartidor);
        em.clear();

        Repartidor releido = repositorio.findById(id).orElseThrow();
        assertThat(releido.getTelefono()).isEqualTo("911222333");
        assertThat(releido.getVehiculo()).isEqualTo(TipoVehiculo.BICICLETA);
        assertThat(releido.getPlaca()).isNull();
        assertThat(releido.isActivo()).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 elimina físicamente un repartidor sin entregas")
    void eliminaSinEntregas() {
        Long id = repositorio.saveAndFlush(repartidor("Juan Pérez")).getId();

        assertThat(entregas.existsByRepartidorId(id)).isFalse();
        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(repositorio.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 no elimina un repartidor que tiene entregas asignadas")
    void noEliminaConEntregas() {
        Repartidor repartidor = repositorio.saveAndFlush(repartidor("Juan Pérez"));
        Entrega entrega = entrega(Modalidad.DELIVERY_LOCAL);
        entrega.setRepartidor(repartidor);
        entregas.saveAndFlush(entrega);

        assertThat(entregas.existsByRepartidorId(repartidor.getId())).isTrue();
        assertThatThrownBy(() -> {
            repositorio.deleteById(repartidor.getId());
            repositorio.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 lista solo los repartidores activos")
    void listaActivos() {
        repositorio.save(repartidor("Juan Pérez"));
        repositorio.save(repartidor("Rosa Díaz"));
        Repartidor inactivo = repartidor("Luis Soto");
        inactivo.setActivo(false);
        repositorio.save(inactivo);
        repositorio.flush();

        assertThat(repositorio.findByActivoTrue()).extracting(Repartidor::getNombres)
                .containsExactlyInAnyOrder("Juan Pérez", "Rosa Díaz");
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un tipo de vehículo fuera de MOTO, AUTO y BICICLETA")
    void rechazaVehiculoInvalido() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO repartidor (nombres, telefono, vehiculo, activo, creado_en, actualizado_en)
                VALUES ('Juan', '987654321', 'CAMION', true, now(), now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
