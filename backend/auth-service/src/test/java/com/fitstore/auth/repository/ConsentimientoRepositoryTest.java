package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Consentimiento;
import com.fitstore.auth.model.TipoConsentimiento;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaRepositorio;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class ConsentimientoRepositoryTest {

    @Autowired ConsentimientoRepository repositorio;
    @Autowired ClienteRepository clientes;
    @Autowired JdbcTemplate jdbc;

    Cliente cliente;

    @BeforeEach
    void cliente() {
        cliente = clientes.saveAndFlush(Datos.cliente("cons@correo.pe", "13131313"));
    }

    private Consentimiento consentimiento(TipoConsentimiento tipo) {
        Consentimiento c = new Consentimiento();
        c.setCliente(cliente);
        c.setTipo(tipo);
        c.setVersionPolitica("2026-10");
        c.setAceptadoEn(Instant.now());
        c.setIp("190.234.10.5");
        return c;
    }

    @Test
    @DisplayName("CA-0a-06 ConsentimientoRepository inserta y lista los consentimientos del cliente")
    void insertaYLista() {
        repositorio.save(consentimiento(TipoConsentimiento.TERMINOS));
        repositorio.save(consentimiento(TipoConsentimiento.PRIVACIDAD));
        repositorio.flush();

        assertThat(repositorio.findByClienteId(cliente.getId())).extracting(Consentimiento::getTipo)
                .containsExactlyInAnyOrder(TipoConsentimiento.TERMINOS, TipoConsentimiento.PRIVACIDAD);
        assertThat(repositorio.findByClienteId(cliente.getId()).get(0).getVersionPolitica()).isEqualTo("2026-10");
    }

    @Test
    @DisplayName("CA-0a-06 ConsentimientoRepository rechaza un tipo fuera de TERMINOS y PRIVACIDAD (CHECK)")
    void rechazaTipoInvalido() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO consentimiento (cliente_id, tipo, version_politica, aceptado_en, ip)
                VALUES (?, 'MARKETING', '1', now(), '1.1.1.1')
                """, cliente.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
