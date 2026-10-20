package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.agencia;
import static com.fitstore.delivery.pruebas.Datos.entrega;
import static com.fitstore.delivery.pruebas.Datos.repartidor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PruebaRepositorio
class EntregaRepositoryTest {

    @Autowired
    EntregaRepository repositorio;

    @Autowired
    RepartidorRepository repartidores;

    @Autowired
    AgenciaRepository agencias;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 inserta una entrega creada desde el evento, con la copia del destinatario y la dirección")
    void inserta() {
        Entrega nueva = entrega(Modalidad.DELIVERY_LOCAL);

        repositorio.saveAndFlush(nueva);
        em.clear();

        Entrega leida = repositorio.findByPedidoId(nueva.getPedidoId()).orElseThrow();
        assertThat(leida.getId()).isEqualTo(nueva.getId());
        assertThat(leida.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(leida.getIntentos()).isZero();
        assertThat(leida.getVersion()).isZero();
        assertThat(leida.getDestinatario().getEmail()).isEqualTo("ana@correo.pe");
        assertThat(leida.getDestinatario().getDni()).isEqualTo("12345678");
        assertThat(leida.getDireccion().getDistrito()).isEqualTo("La Victoria");
        assertThat(leida.getDireccion().getUbigeo()).isEqualTo("150115");
        assertThat(leida.getResumenItems()).isEqualTo("2 x Polo deportivo M");
        assertThat(leida.getBultos()).isEqualTo(1);
        assertThat(leida.getRepartidor()).isNull();
        assertThat(leida.getAgencia()).isNull();
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el estado y la asignación de repartidor y fecha")
    void actualizaEstadoYAsignacion() {
        Repartidor juan = repartidores.saveAndFlush(repartidor("Juan Pérez"));
        Entrega guardada = repositorio.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));
        em.clear();

        Entrega entrega = repositorio.findById(guardada.getId()).orElseThrow();
        entrega.setRepartidor(juan);
        entrega.setFechaProgramada(LocalDate.of(2026, 10, 22));
        entrega.setEstado(EstadoEntrega.ASIGNADA);
        entrega.setIntentos(1);
        repositorio.saveAndFlush(entrega);
        em.clear();

        Entrega releida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(releida.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(releida.getRepartidor().getId()).isEqualTo(juan.getId());
        assertThat(releida.getFechaProgramada()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(releida.getIntentos()).isEqualTo(1);
        assertThat(releida.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el avance por agencia: agencia, código de seguimiento y clave de recojo")
    void actualizaEnvioPorAgencia() {
        Agencia shalom = agencias.saveAndFlush(agencia("Agencia de prueba", 99));
        Entrega guardada = repositorio.saveAndFlush(entrega(Modalidad.AGENCIA));
        em.clear();

        Entrega entrega = repositorio.findById(guardada.getId()).orElseThrow();
        entrega.setAgencia(shalom);
        entrega.setCodigoSeguimiento("12345");
        entrega.setClaveRecojo("4821");
        entrega.setEstado(EstadoEntrega.ENVIADO_AGENCIA);
        repositorio.saveAndFlush(entrega);
        em.clear();

        Entrega releida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(releida.getAgencia().getNombre()).isEqualTo("Agencia de prueba");
        assertThat(releida.getCodigoSeguimiento()).isEqualTo("12345");
        assertThat(releida.getClaveRecojo()).isEqualTo("4821");
        assertThat(releida.getEstado()).isEqualTo(EstadoEntrega.ENVIADO_AGENCIA);
    }

    @Test
    @DisplayName("CA-0a-06 lista con filtros por estado, modalidad, fecha, repartidor y código de pedido")
    void listaConFiltros() {
        Repartidor juan = repartidores.saveAndFlush(repartidor("Juan Pérez"));
        Entrega a = entrega(Modalidad.DELIVERY_LOCAL);
        a.setCodigoPedido("FS-A");
        a.setRepartidor(juan);
        a.setFechaProgramada(LocalDate.of(2026, 10, 22));
        a.setEstado(EstadoEntrega.ASIGNADA);
        Entrega b = entrega(Modalidad.AGENCIA);
        b.setCodigoPedido("FS-B");
        Entrega c = entrega(Modalidad.DELIVERY_LOCAL);
        c.setCodigoPedido("FS-C");
        repositorio.saveAllAndFlush(List.of(a, b, c));

        Specification<Entrega> todas = Specification.unrestricted();
        assertThat(repositorio.findAll(todas)).hasSize(3);
        assertThat(repositorio.findAll((r, q, cb) -> cb.equal(r.get("estado"), EstadoEntrega.ASIGNADA)))
                .extracting(Entrega::getCodigoPedido).containsExactly("FS-A");
        assertThat(repositorio.findAll((r, q, cb) -> cb.equal(r.get("modalidad"), Modalidad.AGENCIA)))
                .extracting(Entrega::getCodigoPedido).containsExactly("FS-B");
        assertThat(repositorio.findAll((r, q, cb) -> cb.equal(r.get("fechaProgramada"), LocalDate.of(2026, 10, 22))))
                .extracting(Entrega::getCodigoPedido).containsExactly("FS-A");
        assertThat(repositorio.findAll((r, q, cb) -> cb.equal(r.get("repartidor").get("id"), juan.getId())))
                .extracting(Entrega::getCodigoPedido).containsExactly("FS-A");
        assertThat(repositorio.findAll((r, q, cb) -> cb.equal(r.get("codigoPedido"), "FS-C")))
                .extracting(Entrega::getCodigoPedido).containsExactly("FS-C");
    }

    @Test
    @DisplayName("CA-0a-06 lista las entregas del día de un repartidor")
    void listaDelDiaPorRepartidor() {
        Repartidor juan = repartidores.saveAndFlush(repartidor("Juan Pérez"));
        Repartidor rosa = repartidores.saveAndFlush(repartidor("Rosa Díaz"));
        LocalDate dia = LocalDate.of(2026, 10, 22);
        repositorio.saveAllAndFlush(List.of(
                asignada("FS-1", juan, dia),
                asignada("FS-2", juan, dia),
                asignada("FS-3", juan, dia.plusDays(1)),
                asignada("FS-4", rosa, dia)));

        assertThat(repositorio.findByFechaProgramadaAndRepartidorId(dia, juan.getId()))
                .extracting(Entrega::getCodigoPedido).containsExactlyInAnyOrder("FS-1", "FS-2");
    }

    private Entrega asignada(String codigo, Repartidor repartidor, LocalDate fecha) {
        Entrega entrega = entrega(Modalidad.DELIVERY_LOCAL);
        entrega.setCodigoPedido(codigo);
        entrega.setRepartidor(repartidor);
        entrega.setFechaProgramada(fecha);
        entrega.setEstado(EstadoEntrega.ASIGNADA);
        return entrega;
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una segunda entrega para el mismo pedido")
    void rechazaPedidoDuplicado() {
        Entrega primera = repositorio.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));
        Entrega repetida = entrega(Modalidad.DELIVERY_LOCAL);
        repetida.setPedidoId(primera.getPedidoId());

        assertThatThrownBy(() -> repositorio.saveAndFlush(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una entrega sin bultos")
    void rechazaBultosCero() {
        Entrega sinBultos = entrega(Modalidad.DELIVERY_LOCAL);
        sinBultos.setBultos(0);

        assertThatThrownBy(() -> repositorio.saveAndFlush(sinBultos)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un número de intentos negativo")
    void rechazaIntentosNegativos() {
        Entrega negativa = entrega(Modalidad.DELIVERY_LOCAL);
        negativa.setIntentos(-1);

        assertThatThrownBy(() -> repositorio.saveAndFlush(negativa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un estado fuera de los nueve valores del contrato")
    void rechazaEstadoInvalido() {
        repositorio.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));

        assertThatThrownBy(() -> jdbc.update("UPDATE entrega SET estado = 'PERDIDA'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una modalidad distinta de DELIVERY_LOCAL y AGENCIA")
    void rechazaModalidadInvalida() {
        repositorio.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));

        assertThatThrownBy(() -> jdbc.update("UPDATE entrega SET modalidad = 'RECOJO'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 detecta la modificación concurrente con el control de versión")
    void detectaModificacionConcurrente() {
        Entrega guardada = repositorio.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));
        em.clear();
        Entrega vieja = repositorio.findById(guardada.getId()).orElseThrow();

        jdbc.update("UPDATE entrega SET version = version + 1");
        vieja.setIntentos(1);

        assertThatThrownBy(() -> repositorio.saveAndFlush(vieja)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
