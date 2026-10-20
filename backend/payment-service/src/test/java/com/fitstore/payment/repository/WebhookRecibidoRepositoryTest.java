package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.WebhookRecibido;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class WebhookRecibidoRepositoryTest {

    @Autowired WebhookRecibidoRepository repositorio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 inserta un webhook con su payload jsonb y lo busca por proveedor y evento")
    void inserta() {
        repositorio.saveAndFlush(Datos.webhook(Proveedor.MERCADO_PAGO, "evt-1"));
        em.clear();

        WebhookRecibido leido = repositorio.findByProveedorAndEventoIdProveedor(Proveedor.MERCADO_PAGO, "evt-1")
                .orElseThrow();
        assertThat(leido.getPayload()).containsEntry("type", "payment.created");
        assertThat(leido.getRecibidoEn()).isNotNull();
        assertThat(leido.getProcesadoEn()).isNull();
        assertThat(repositorio.findByProveedorAndEventoIdProveedor(Proveedor.CULQI, "evt-1")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 marca el webhook como procesado y lista solo los pendientes")
    void marcaProcesado() {
        WebhookRecibido uno = repositorio.save(Datos.webhook(Proveedor.CULQI, "evt-1"));
        WebhookRecibido dos = repositorio.saveAndFlush(Datos.webhook(Proveedor.CULQI, "evt-2"));
        uno.setProcesadoEn(Instant.now());
        repositorio.flush();
        em.clear();

        assertThat(repositorio.findByProcesadoEnIsNull()).extracting(WebhookRecibido::getId)
                .contains(dos.getId()).doesNotContain(uno.getId());
    }

    @Test
    @DisplayName("CA-0a-06 el mismo id de evento puede venir de otro proveedor")
    void mismoIdOtroProveedor() {
        repositorio.save(Datos.webhook(Proveedor.CULQI, "evt-1"));
        repositorio.saveAndFlush(Datos.webhook(Proveedor.MERCADO_PAGO, "evt-1"));

        assertThat(repositorio.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza el mismo evento del mismo proveedor (webhook duplicado)")
    void rechazaDuplicado() {
        repositorio.saveAndFlush(Datos.webhook(Proveedor.CULQI, "evt-1"));

        WebhookRecibido repetido = Datos.webhook(Proveedor.CULQI, "evt-1");
        assertThatThrownBy(() -> repositorio.saveAndFlush(repetido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un proveedor fuera de los valores permitidos (CHECK proveedor)")
    void rechazaProveedorInvalido() {
        WebhookRecibido guardado = repositorio.saveAndFlush(Datos.webhook(Proveedor.CULQI, "evt-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE webhook_recibido SET proveedor = 'PAYPAL' WHERE id = ?",
                guardado.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
