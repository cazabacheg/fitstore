package com.fitstore.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.persistence.JsonPersistenciaAutoConfiguration;
import com.fitstore.notification.model.Modalidad;
import com.fitstore.notification.model.PedidoResumen;
import com.fitstore.notification.pruebas.PostgresInicializador;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresInicializador.class)
@Import(JsonPersistenciaAutoConfiguration.class)
class PedidoResumenRepositoryTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    @Autowired
    PedidoResumenRepository repositorio;

    @Autowired
    TestEntityManager em;

    private PedidoResumen resumen(String codigo, Modalidad modalidad, Instant creadoEn) {
        PedidoResumen p = new PedidoResumen();
        p.setPedidoId(UUID.randomUUID());
        p.setCodigo(codigo);
        p.setEmail("lucia@correo.pe");
        p.setNombre("Lucía Quispe");
        p.setItems(List.of(Map.of("sku", "LEG-NEG-M", "producto", "Legging Power", "cantidad", 2,
                "total", new BigDecimal("119.80"))));
        p.setTotales(Map.of("subtotal", new BigDecimal("119.80"), "costoEnvio", new BigDecimal("10.00"),
                "total", new BigDecimal("129.80")));
        p.setModalidad(modalidad);
        p.setCreadoEn(creadoEn);
        return p;
    }

    private PedidoResumen guardar(PedidoResumen p) {
        repositorio.saveAndFlush(p);
        em.clear();
        return p;
    }

    @Test
    @DisplayName("CA-0a-06 PedidoResumenRepository inserta el resumen de PedidoCreado y lo encuentra por pedido")
    void insertaYBuscaPorPedido() {
        PedidoResumen p = guardar(resumen("FS-000124", Modalidad.DELIVERY_LOCAL, AHORA));

        PedidoResumen leido = repositorio.findById(p.getPedidoId()).orElseThrow();

        assertThat(leido.getCodigo()).isEqualTo("FS-000124");
        assertThat(leido.getModalidad()).isEqualTo(Modalidad.DELIVERY_LOCAL);
        assertThat(leido.getItems()).hasSize(1);
        assertThat(leido.getItems().get(0)).containsEntry("sku", "LEG-NEG-M");
        assertThat(leido.getTotales()).containsKey("total");
    }

    @Test
    @DisplayName("CA-0a-06 PedidoResumenRepository purga los resúmenes de más de 180 días y conserva el resto")
    void purgaPorAntiguedad() {
        Instant limite = AHORA.minus(180, ChronoUnit.DAYS);
        guardar(resumen("VIEJO", Modalidad.AGENCIA, limite.minusSeconds(1)));
        guardar(resumen("LIMITE", Modalidad.RECOJO, limite));
        guardar(resumen("NUEVO", Modalidad.RECOJO, AHORA));

        int borrados = repositorio.purgarAnterioresA(limite);
        em.clear();

        assertThat(borrados).isEqualTo(1);
        assertThat(repositorio.findAll()).extracting(PedidoResumen::getCodigo)
                .containsExactlyInAnyOrder("LIMITE", "NUEVO");
    }

    @Test
    @DisplayName("CA-0a-06 PedidoResumenRepository rechaza una modalidad fuera del CHECK")
    void rechazaModalidadInvalida() {
        guardar(resumen("FS-000001", Modalidad.AGENCIA, AHORA));
        var manager = em.getEntityManager();

        assertThatThrownBy(() -> {
            manager.createNativeQuery("update pedido_resumen set modalidad = 'DRON'").executeUpdate();
            manager.flush();
        }).hasMessageContaining("ck_pedido_resumen_modalidad");
    }

    @Test
    @DisplayName("CA-0a-06 PedidoResumenRepository no admite el mismo pedido dos veces")
    void rechazaDuplicado() {
        guardar(resumen("FS-000002", Modalidad.AGENCIA, AHORA));
        var manager = em.getEntityManager();

        assertThatThrownBy(() -> {
            manager.createNativeQuery("insert into pedido_resumen select * from pedido_resumen").executeUpdate();
        }).hasMessageContaining("pedido_resumen_pkey");
    }

    @Test
    @DisplayName("CA-0a-06 PedidoResumenRepository elimina un resumen")
    void eliminaUnResumen() {
        PedidoResumen p = guardar(resumen("FS-000003", Modalidad.RECOJO, AHORA));

        repositorio.deleteById(p.getPedidoId());
        repositorio.flush();

        assertThat(repositorio.findById(p.getPedidoId())).isEmpty();
    }
}
