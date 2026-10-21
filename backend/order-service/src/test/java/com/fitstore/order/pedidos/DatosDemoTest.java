package com.fitstore.order.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pruebas.EntornoDemo;
import com.fitstore.order.pruebas.Sesiones;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Datos de demostración de order (SPEC-4 sección 6) con el perfil dev: pedidos de los 4 clientes de
 * {@code docs/demo/ids-demo.md}, los ids que usa delivery y la secuencia de códigos. Usa su propia base de datos.
 */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/dev")
@AutoConfigureMockMvc
@ActiveProfiles({"sin-config-server", "dev"})
@ContextConfiguration(initializers = EntornoDemo.class)
class DatosDemoTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PedidoRepository pedidos;

    private static final UUID LUCIA = UUID.fromString("35a11d4f-054a-44a5-8ac4-cf0ed4020f8c");

    @Test
    @DisplayName("Demo: 11 pedidos de los 4 clientes, uno por expirar en 10 minutos, recojo, agencia con seguimiento y entregados")
    void pedidosDeDemostracion() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido", Integer.class)).isEqualTo(11);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT cliente_id) FROM pedido", Integer.class)).isEqualTo(4);
        Map<String, Object> porExpirar = jdbc.queryForMap("SELECT estado, expira_en > now() + interval '9 minutes' AS ok, "
                + "expira_en < now() + interval '11 minutes' AS ok2 FROM pedido WHERE codigo = 'FS-000105'");
        assertThat(porExpirar).containsEntry("estado", "PENDIENTE_PAGO").containsEntry("ok", true).containsEntry("ok2", true);
        assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE modalidad = 'RECOJO' AND codigo = 'FS-000108'", String.class))
                .isEqualTo("LISTO_RECOJO");
        assertThat(jdbc.queryForObject("SELECT codigo_seguimiento FROM pedido WHERE codigo = 'FS-000103'", String.class))
                .isEqualTo("SH-2026-48121");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido WHERE estado = 'ENTREGADO' "
                + "AND creado_en > date_trunc('month', now()) - interval '30 days'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido_reembolso WHERE estado = 'COMPLETADO'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido p WHERE NOT EXISTS "
                + "(SELECT 1 FROM pedido_historial h WHERE h.pedido_id = p.id)", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido WHERE total <> subtotal - descuento_total + costo_envio",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("Demo: los 4 pedidos que delivery espera tienen el id, código, cliente y estado acordados")
    void pedidosDeDelivery() {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT id::text AS id, codigo, cliente_id::text AS cliente, "
                + "estado, modalidad, dir_ubigeo FROM pedido WHERE codigo IN ('FS-000101','FS-000102','FS-000103','FS-000104') "
                + "ORDER BY codigo");
        assertThat(filas).hasSize(4);
        assertThat(filas.get(0)).containsEntry("id", "3e13f37f-db57-44c2-8ac1-66d7ce378e0b")
                .containsEntry("cliente", "35a11d4f-054a-44a5-8ac4-cf0ed4020f8c").containsEntry("estado", "ENVIADO")
                .containsEntry("modalidad", "DELIVERY_LOCAL").containsEntry("dir_ubigeo", "150122");
        assertThat(filas.get(1)).containsEntry("id", "33900490-eace-44ab-a6b3-391a28202b57")
                .containsEntry("cliente", "41f4b1c9-00e0-4816-91b9-a1a143a69df9").containsEntry("estado", "ENTREGADO")
                .containsEntry("dir_ubigeo", "150101");
        assertThat(filas.get(2)).containsEntry("id", "5928f8f8-ed74-413e-91f4-d3a6115f6afc")
                .containsEntry("cliente", "652dcca9-9c40-434a-9d2d-33e44d1dcc16").containsEntry("estado", "ENVIADO")
                .containsEntry("modalidad", "AGENCIA").containsEntry("dir_ubigeo", "040101");
        assertThat(filas.get(3)).containsEntry("id", "b7655951-8c1f-4f51-9a90-39534aa106d9")
                .containsEntry("cliente", "a3d50456-6d33-4e6e-80fc-62563fc46808").containsEntry("estado", "LISTO_DESPACHO")
                .containsEntry("dir_ubigeo", "070101");
    }

    @Test
    @DisplayName("Demo: el próximo pedido que se cree sigue después del último de la demo (FS-000112)")
    void secuenciaDeCodigos() {
        assertThat(pedidos.siguienteNumeroCodigo()).isEqualTo(112);
    }

    @Test
    @DisplayName("Demo: la API muestra los pedidos de Lucía y el reembolso parcial; el backoffice, los contadores por estado")
    void apiConDatosDeDemo() throws Exception {
        mvc.perform(get("/api/v1/pedidos").with(Sesiones.cliente(LUCIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(4)));
        mvc.perform(get("/api/v1/pedidos/FS-000109").with(Sesiones.cliente(LUCIA)))
                .andExpect(jsonPath("$.data.estadoReembolso").value("PARCIAL"))
                .andExpect(jsonPath("$.data.historial[?(@.tipo=='REEMBOLSO_COMPLETADO')]", hasSize(1)));
        mvc.perform(get("/api/v1/admin/pedidos").with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.contadores.ENTREGADO").value(2))
                .andExpect(jsonPath("$.data.contadores.PAGADO").value(1))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE_PAGO").value(1))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("PAGADO"));
    }
}
