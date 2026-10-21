package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.entrega;
import static com.fitstore.delivery.pruebas.Datos.repartidor;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.delivery.dto.FiltroEntregas;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Filtros del listado, orden de pendientes primero y contadores por estado (CA-6-31 y CA-6-32). */
@PruebaRepositorio
class EntregaSpecificationsTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Autowired EntregaRepository entregas;
    @Autowired TestEntityManager em;

    Repartidor juan;
    Repartidor rosa;

    @BeforeEach
    void datos() {
        juan = em.persist(repartidor("Juan Pérez"));
        rosa = em.persist(repartidor("Rosa Díaz"));
        guardar("FS-000401", Modalidad.DELIVERY_LOCAL, EstadoEntrega.ASIGNADA, juan, HOY);
        guardar("FS-000402", Modalidad.DELIVERY_LOCAL, EstadoEntrega.EN_RUTA, juan, HOY);
        guardar("FS-000403", Modalidad.DELIVERY_LOCAL, EstadoEntrega.ASIGNADA, rosa, HOY.plusDays(1));
        guardar("FS-000404", Modalidad.DELIVERY_LOCAL, EstadoEntrega.PENDIENTE_ASIGNACION, null, null);
        guardar("FS-000405", Modalidad.AGENCIA, EstadoEntrega.PENDIENTE_ENVIO, null, null);
        guardar("FS-000406", Modalidad.AGENCIA, EstadoEntrega.ENVIADO_AGENCIA, null, null);
        em.flush();
        em.clear();
    }

    private void guardar(String codigo, Modalidad modalidad, EstadoEntrega estado, Repartidor repartidor, LocalDate fecha) {
        Entrega e = entrega(modalidad);
        e.setCodigoPedido(codigo);
        e.setEstado(estado);
        e.setRepartidor(repartidor);
        e.setFechaProgramada(fecha);
        em.persist(e);
    }

    private List<String> codigos(FiltroEntregas filtro) {
        return entregas.findAll(EntregaSpecifications.filtrar(filtro, true), PageRequest.of(0, 50, Sort.by("codigoPedido")))
                .map(Entrega::getCodigoPedido).getContent();
    }

    @Test
    @DisplayName("CA-6-32 filtra por modalidad, estado, fecha programada, repartidor y código de pedido")
    void filtrosSimples() {
        assertThat(codigos(new FiltroEntregas(Modalidad.AGENCIA, null, null, null, null)))
                .containsExactly("FS-000405", "FS-000406");
        assertThat(codigos(new FiltroEntregas(null, EstadoEntrega.ASIGNADA, null, null, null)))
                .containsExactly("FS-000401", "FS-000403");
        assertThat(codigos(new FiltroEntregas(null, null, HOY, null, null))).containsExactly("FS-000401", "FS-000402");
        assertThat(codigos(new FiltroEntregas(null, null, null, rosa.getId(), null))).containsExactly("FS-000403");
        assertThat(codigos(new FiltroEntregas(null, null, null, null, "000404"))).containsExactly("FS-000404");
        assertThat(codigos(new FiltroEntregas(null, null, null, null, "fs-00040"))).hasSize(6);
    }

    @Test
    @DisplayName("CA-6-32 combina todos los filtros: solo quedan las que cumplen todos")
    void filtrosCombinados() {
        assertThat(codigos(new FiltroEntregas(Modalidad.DELIVERY_LOCAL, EstadoEntrega.ASIGNADA, HOY, juan.getId(), "401")))
                .containsExactly("FS-000401");
        assertThat(codigos(new FiltroEntregas(Modalidad.DELIVERY_LOCAL, EstadoEntrega.ASIGNADA, HOY, rosa.getId(), null)))
                .isEmpty();
    }

    @Test
    @DisplayName("CA-6-31 los contadores cuentan por estado dentro de la pestaña, sin aplicar el filtro de estado")
    void contadores() {
        FiltroEntregas pestanaLocal = new FiltroEntregas(Modalidad.DELIVERY_LOCAL, EstadoEntrega.EN_RUTA, null, null, null);

        Map<EstadoEntrega, Long> cuentas = entregas.contarPorEstado(EntregaSpecifications.filtrar(pestanaLocal, false));

        assertThat(cuentas).containsEntry(EstadoEntrega.ASIGNADA, 2L).containsEntry(EstadoEntrega.EN_RUTA, 1L)
                .containsEntry(EstadoEntrega.PENDIENTE_ASIGNACION, 1L).doesNotContainKey(EstadoEntrega.PENDIENTE_ENVIO);
    }

    @Test
    @DisplayName("CA-6-31 sin orden pedido, las pendientes de asignar o de enviar van primero")
    void pendientesPrimero() {
        List<String> codigos = entregas.findAll(EntregaSpecifications.filtrar(new FiltroEntregas(null, null, null, null, null), true)
                .and(EntregaSpecifications.pendientesPrimero()), PageRequest.of(0, 50)).map(Entrega::getCodigoPedido).getContent();

        assertThat(codigos.subList(0, 2)).containsExactlyInAnyOrder("FS-000404", "FS-000405");
        assertThat(codigos).hasSize(6);
    }
}
