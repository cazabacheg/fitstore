package com.fitstore.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** El script de demostración de {@code db/dev} (perfiles dev y demo) carga sobre el esquema con los pedidos fijos (SPEC-6 sección 6). */
@PruebaRepositorio
class DatosDemoTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("la demo trae 3 repartidores (uno inactivo) y las 4 entregas de los pedidos de demostración")
    void cargaLaDemo() throws Exception {
        // Se ejecuta el script dentro de la transacción de la prueba (se revierte): Flyway no mezcla db/dev con las demás pruebas
        String script = StreamUtils.copyToString(new ClassPathResource("db/dev/R__entregas_demo.sql").getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8);
        jdbc.execute(script);

        assertThat(jdbc.queryForList("SELECT nombres FROM repartidor WHERE id BETWEEN 101 AND 103 AND activo", String.class))
                .hasSize(2);
        List<String> estados = jdbc.queryForList(
                "SELECT codigo_pedido || ':' || estado FROM entrega WHERE codigo_pedido LIKE 'FS-0001%' ORDER BY codigo_pedido",
                String.class);
        assertThat(estados).containsExactly("FS-000101:EN_RUTA", "FS-000102:ENTREGADA", "FS-000103:ENVIADO_AGENCIA",
                "FS-000104:PENDIENTE_ASIGNACION");
        assertThat(jdbc.queryForObject("SELECT codigo_seguimiento FROM entrega WHERE codigo_pedido = 'FS-000103'", String.class))
                .isNotBlank();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entrega_evento", Integer.class)).isEqualTo(10);
    }
}
