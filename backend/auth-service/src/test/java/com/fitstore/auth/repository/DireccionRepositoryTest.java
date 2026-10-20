package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Direccion;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class DireccionRepositoryTest {

    @Autowired DireccionRepository repositorio;
    @Autowired ClienteRepository clientes;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    Cliente cliente;

    @BeforeEach
    void cliente() {
        cliente = clientes.saveAndFlush(Datos.cliente("dir@correo.pe", "10101010"));
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository inserta una dirección con su auditoría")
    void inserta() {
        Direccion d = repositorio.saveAndFlush(Datos.direccion(cliente, "Casa", true));

        Direccion leida = repositorio.findById(d.getId()).orElseThrow();
        assertThat(leida.getAlias()).isEqualTo("Casa");
        assertThat(leida.getUbigeo()).isEqualTo("150101");
        assertThat(leida.getTelefonoContacto()).isNull();
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository actualiza una dirección")
    void actualiza() {
        Direccion d = repositorio.saveAndFlush(Datos.direccion(cliente, "Casa", true));
        d.setAlias("Trabajo");
        d.setReferencia("Frente al parque");
        d.setTelefonoContacto("912345678");
        repositorio.saveAndFlush(d);

        Direccion leida = repositorio.findById(d.getId()).orElseThrow();
        assertThat(leida.getAlias()).isEqualTo("Trabajo");
        assertThat(leida.getReferencia()).isEqualTo("Frente al parque");
        assertThat(leida.getTelefonoContacto()).isEqualTo("912345678");
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository elimina de forma física")
    void eliminaFisicamente() {
        Direccion d = repositorio.saveAndFlush(Datos.direccion(cliente, "Casa", true));

        repositorio.deleteById(d.getId());
        repositorio.flush();

        assertThat(repositorio.findById(d.getId())).isEmpty();
        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository lista y cuenta las direcciones por cliente")
    void listaPorCliente() {
        Cliente otro = clientes.saveAndFlush(Datos.cliente("otro@correo.pe", "20202020"));
        repositorio.save(Datos.direccion(cliente, "Casa", true));
        repositorio.save(Datos.direccion(cliente, "Trabajo", false));
        repositorio.save(Datos.direccion(otro, "Casa", true));
        repositorio.flush();

        assertThat(repositorio.findByClienteId(cliente.getId())).extracting(Direccion::getAlias)
                .containsExactlyInAnyOrder("Casa", "Trabajo");
        assertThat(repositorio.countByClienteId(cliente.getId())).isEqualTo(2);
        assertThat(repositorio.findByClienteIdAndPredeterminadaTrue(cliente.getId())).get()
                .extracting(Direccion::getAlias).isEqualTo("Casa");
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository permite una sola predeterminada por cliente (índice parcial)")
    void unaSolaPredeterminada() {
        repositorio.saveAndFlush(Datos.direccion(cliente, "Casa", true));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.direccion(cliente, "Trabajo", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository permite varias no predeterminadas y una predeterminada por cada cliente")
    void variasNoPredeterminadas() {
        Cliente otro = clientes.saveAndFlush(Datos.cliente("otro2@correo.pe", "30303030"));
        repositorio.save(Datos.direccion(cliente, "Casa", true));
        repositorio.save(Datos.direccion(cliente, "A", false));
        repositorio.save(Datos.direccion(cliente, "B", false));
        repositorio.save(Datos.direccion(otro, "Casa", true));
        repositorio.flush();

        assertThat(repositorio.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository rechaza un ubigeo que no tiene 6 dígitos (CHECK)")
    void rechazaUbigeoInvalido() {
        Direccion d = Datos.direccion(cliente, "Casa", true);
        d.setUbigeo("15A101");

        assertThatThrownBy(() -> repositorio.saveAndFlush(d)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 DireccionRepository rechaza un cliente que no existe (FK)")
    void rechazaClienteInexistente() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO direccion (cliente_id, alias, ubigeo, direccion, predeterminada, creado_en, actualizado_en)
                VALUES (gen_random_uuid(), 'Casa', '150101', 'Jr. Lima 1', false, now(), now())
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
