package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class TransaccionRepositoryTest {

    @Autowired TransaccionRepository repositorio;
    @Autowired IntencionPagoRepository intenciones;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private IntencionPago intencion() {
        return intenciones.saveAndFlush(Datos.intencion());
    }

    @Test
    @DisplayName("CA-0a-06 inserta varios intentos de cobro de una intención y los lista en orden")
    void insertaIntentos() {
        IntencionPago intencion = intencion();
        repositorio.saveAndFlush(Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));
        Transaccion segundo = Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, "k-2");
        segundo.setRespuesta(Map.of("codigo", "ok"));
        repositorio.saveAndFlush(segundo);
        em.clear();

        List<Transaccion> intentos = repositorio.findByIntencionIdOrderByCreadoEnAsc(intencion.getId());
        assertThat(intentos).extracting(Transaccion::getIdempotencyKey).containsExactly("k-1", "k-2");
        assertThat(intentos.get(1).getRespuesta()).containsEntry("codigo", "ok");
        assertThat(repositorio.findByIntencionIdAndIdempotencyKey(intencion.getId(), "k-2")).isPresent();
    }

    @Test
    @DisplayName("CA-0a-06 aprueba y rechaza una transacción")
    void apruebaYRechaza() {
        IntencionPago intencion = intencion();
        Transaccion aprobada = repositorio.saveAndFlush(
                Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, "k-1"));
        Transaccion rechazada = repositorio.saveAndFlush(
                Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, "k-2"));

        aprobada.setEstado(EstadoTransaccion.APROBADA);
        aprobada.setIdProveedor("chr_123");
        rechazada.setEstado(EstadoTransaccion.RECHAZADA);
        rechazada.setCodigoRespuesta("card_declined");
        rechazada.setMensaje("Fondos insuficientes");
        repositorio.flush();
        em.clear();

        assertThat(repositorio.findById(aprobada.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransaccion.APROBADA);
        Transaccion leida = repositorio.findById(rechazada.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(leida.getMensaje()).isEqualTo("Fondos insuficientes");
    }

    @Test
    @DisplayName("CA-0a-06 lista las transacciones PROCESANDO más antiguas que un instante")
    void listaProcesandoAntiguas() {
        IntencionPago intencion = intencion();
        Transaccion antigua = Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, "k-1");
        antigua.setActualizadoEn(Instant.now().minus(10, ChronoUnit.MINUTES));
        repositorio.save(antigua);
        repositorio.save(Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, "k-2"));
        Transaccion rechazadaAntigua = Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-3");
        rechazadaAntigua.setActualizadoEn(Instant.now().minus(10, ChronoUnit.MINUTES));
        repositorio.saveAndFlush(rechazadaAntigua);

        List<Transaccion> pendientes = repositorio.findByEstadoAndActualizadoEnBefore(
                EstadoTransaccion.PROCESANDO, Instant.now().minus(2, ChronoUnit.MINUTES));

        assertThat(pendientes).extracting(Transaccion::getIdempotencyKey).contains("k-1").doesNotContain("k-2", "k-3");
    }

    @Test
    @DisplayName("CA-0a-06 la segunda transacción APROBADA de la misma intención la rechaza el índice único parcial")
    void rechazaSegundaAprobada() {
        IntencionPago intencion = intencion();
        repositorio.saveAndFlush(Datos.transaccion(intencion.getId(), EstadoTransaccion.APROBADA, "k-1"));

        Transaccion segunda = Datos.transaccion(intencion.getId(), EstadoTransaccion.APROBADA, "k-2");
        assertThatThrownBy(() -> repositorio.saveAndFlush(segunda)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 una aprobada convive con intentos rechazados de la misma intención")
    void aprobadaConviveConRechazadas() {
        IntencionPago intencion = intencion();
        repositorio.save(Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));
        repositorio.save(Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-2"));
        repositorio.saveAndFlush(Datos.transaccion(intencion.getId(), EstadoTransaccion.APROBADA, "k-3"));

        assertThat(repositorio.findByIntencionIdOrderByCreadoEnAsc(intencion.getId())).hasSize(3);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una Idempotency-Key repetida en la misma intención")
    void rechazaIdempotencyKeyRepetida() {
        IntencionPago intencion = intencion();
        repositorio.saveAndFlush(Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));

        Transaccion repetida = Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1");
        assertThatThrownBy(() -> repositorio.saveAndFlush(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una transacción de una intención que no existe (clave foránea)")
    void rechazaIntencionInexistente() {
        Transaccion huerfana = Datos.transaccion(java.util.UUID.randomUUID(), EstadoTransaccion.RECHAZADA, "k-1");

        assertThatThrownBy(() -> repositorio.saveAndFlush(huerfana)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un proveedor fuera de los valores permitidos (CHECK proveedor)")
    void rechazaProveedorInvalido() {
        IntencionPago intencion = intencion();
        Transaccion guardada = repositorio.saveAndFlush(
                Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE transaccion SET proveedor = 'PAYPAL' WHERE id = ?",
                guardada.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un medio fuera de los valores permitidos (CHECK medio)")
    void rechazaMedioInvalido() {
        IntencionPago intencion = intencion();
        Transaccion guardada = repositorio.saveAndFlush(
                Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE transaccion SET medio = 'EFECTIVO' WHERE id = ?",
                guardada.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un estado fuera de los valores permitidos (CHECK estado)")
    void rechazaEstadoInvalido() {
        IntencionPago intencion = intencion();
        Transaccion guardada = repositorio.saveAndFlush(
                Datos.transaccion(intencion.getId(), EstadoTransaccion.RECHAZADA, "k-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE transaccion SET estado = 'INVENTADO' WHERE id = ?",
                guardada.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
