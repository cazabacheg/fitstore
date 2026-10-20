package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.MotivoReembolso;
import com.fitstore.payment.model.Reembolso;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class ReembolsoRepositoryTest {

    @Autowired ReembolsoRepository repositorio;
    @Autowired IntencionPagoRepository intenciones;
    @Autowired TransaccionRepository transacciones;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private IntencionPago intencion;
    private Transaccion cargo;

    @org.junit.jupiter.api.BeforeEach
    void preparar() {
        intencion = intenciones.saveAndFlush(Datos.intencion());
        cargo = transacciones.saveAndFlush(Datos.transaccion(intencion.getId(), EstadoTransaccion.APROBADA, "k-1"));
    }

    private Reembolso nuevo(MotivoReembolso motivo) {
        return Datos.reembolso(intencion.getId(), cargo.getId(), motivo);
    }

    @Test
    @DisplayName("CA-0a-06 inserta un reembolso PENDIENTE sin intentos")
    void inserta() {
        Reembolso guardado = repositorio.saveAndFlush(nuevo(MotivoReembolso.CANCELACION));
        em.clear();

        Reembolso leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoReembolso.PENDIENTE);
        assertThat(leido.getIntentos()).isZero();
        assertThat(leido.getMonto()).isEqualByComparingTo("50.00");
        assertThat(leido.getCreadoEn()).isNotNull();
        assertThat(repositorio.findByIntencionId(intencion.getId())).hasSize(1);
    }

    @Test
    @DisplayName("CA-0a-06 actualiza los intentos, el próximo intento y el estado")
    void actualiza() {
        Reembolso guardado = repositorio.saveAndFlush(nuevo(MotivoReembolso.CANCELACION));
        guardado.setIntentos(4);
        guardado.setEstado(EstadoReembolso.FALLIDO);
        guardado.setUltimoError("Tiempo de espera agotado");
        guardado.setProximoIntentoEn(null);
        repositorio.flush();
        em.clear();

        Reembolso leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getIntentos()).isEqualTo(4);
        assertThat(leido.getEstado()).isEqualTo(EstadoReembolso.FALLIDO);
        assertThat(leido.getUltimoError()).isEqualTo("Tiempo de espera agotado");
        assertThat(repositorio.findByEstado(EstadoReembolso.FALLIDO)).extracting(Reembolso::getId)
                .contains(guardado.getId());
    }

    @Test
    @DisplayName("CA-0a-06 lista los pendientes cuyo próximo intento ya llegó")
    void listaPendientesPorProximoIntento() {
        Instant ahora = Instant.now();
        Reembolso vencido = nuevo(MotivoReembolso.CANCELACION);
        vencido.setProximoIntentoEn(ahora.minus(1, ChronoUnit.MINUTES));
        repositorio.save(vencido);
        Reembolso futuro = nuevo(MotivoReembolso.POSTVENTA);
        futuro.setSolicitudId(UUID.randomUUID());
        futuro.setProximoIntentoEn(ahora.plus(10, ChronoUnit.MINUTES));
        repositorio.save(futuro);
        Reembolso sinFecha = nuevo(MotivoReembolso.POSTVENTA);
        sinFecha.setSolicitudId(UUID.randomUUID());
        repositorio.saveAndFlush(sinFecha);

        assertThat(repositorio.pendientesParaProcesar(ahora)).extracting(Reembolso::getId)
                .contains(vencido.getId(), sinFecha.getId()).doesNotContain(futuro.getId());
    }

    @Test
    @DisplayName("CA-0a-06 varios reembolsos postventa conviven en la misma intención y se buscan por solicitud")
    void variosPostventa() {
        Reembolso uno = nuevo(MotivoReembolso.POSTVENTA);
        uno.setSolicitudId(UUID.randomUUID());
        Reembolso dos = nuevo(MotivoReembolso.POSTVENTA);
        dos.setSolicitudId(UUID.randomUUID());
        repositorio.save(uno);
        repositorio.saveAndFlush(dos);

        assertThat(repositorio.findByIntencionId(intencion.getId())).hasSize(2);
        assertThat(repositorio.findBySolicitudId(dos.getSolicitudId())).get().extracting(Reembolso::getId)
                .isEqualTo(dos.getId());
    }

    @Test
    @DisplayName("CA-0a-06 rechaza dos reembolsos con la misma solicitud (solicitud_id único)")
    void rechazaSolicitudDuplicada() {
        UUID solicitud = UUID.randomUUID();
        Reembolso uno = nuevo(MotivoReembolso.POSTVENTA);
        uno.setSolicitudId(solicitud);
        repositorio.saveAndFlush(uno);

        Reembolso repetido = nuevo(MotivoReembolso.POSTVENTA);
        repetido.setSolicitudId(solicitud);
        assertThatThrownBy(() -> repositorio.saveAndFlush(repetido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un segundo reembolso de cancelación o pago no aplicable en la misma intención")
    void rechazaSegundoTotal() {
        repositorio.saveAndFlush(nuevo(MotivoReembolso.CANCELACION));

        Reembolso otro = nuevo(MotivoReembolso.PAGO_NO_APLICABLE);
        assertThatThrownBy(() -> repositorio.saveAndFlush(otro)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un motivo fuera de los valores permitidos (CHECK motivo)")
    void rechazaMotivoInvalido() {
        Reembolso guardado = repositorio.saveAndFlush(nuevo(MotivoReembolso.CANCELACION));

        assertThatThrownBy(() -> jdbc.update("UPDATE reembolso SET motivo = 'CAPRICHO' WHERE id = ?",
                guardado.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un estado fuera de los valores permitidos (CHECK estado)")
    void rechazaEstadoInvalido() {
        Reembolso guardado = repositorio.saveAndFlush(nuevo(MotivoReembolso.CANCELACION));

        assertThatThrownBy(() -> jdbc.update("UPDATE reembolso SET estado = 'INVENTADO' WHERE id = ?",
                guardado.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un reembolso sin transacción de cargo (transaccion_id obligatorio)")
    void rechazaSinTransaccion() {
        Reembolso sinCargo = nuevo(MotivoReembolso.CANCELACION);
        sinCargo.setTransaccionId(null);

        assertThatThrownBy(() -> repositorio.saveAndFlush(sinCargo)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
