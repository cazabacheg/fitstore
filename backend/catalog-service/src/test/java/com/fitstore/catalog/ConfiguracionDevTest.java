package com.fitstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.catalog.pruebas.PostgresAparte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * El servicio arranca con los YAML reales de {@code infra/config-repo} y el perfil dev:
 * Flyway aplica V1 y V2 y también los datos de demostración de {@code classpath:db/dev} (spec 2), por eso usa su propia base:
 * la demo no debe mezclarse con la base que comparten las demás pruebas. El almacenamiento apunta a un endpoint de mentira.
 */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/catalog-service.yml,file:../../infra/config-repo/catalog-service-dev.yml",
    "fitstore.outbox.habilitado=false",
    "fitstore.storage.habilitado=true",
    "fitstore.storage.endpoint=http://localhost:9",
    "fitstore.storage.access-key=GKprueba",
    "fitstore.storage.secret-key=clave-de-prueba-sin-valor",
    "fitstore.storage.media-base-url=http://media.prueba/media",
    "fitstore.catalogo.demo.subir-imagenes=false"
})
@ActiveProfiles({"sin-config-server", "dev"})
@ContextConfiguration(initializers = PostgresAparte.class)
class ConfiguracionDevTest {

    @Autowired Environment entorno;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-05 con el perfil dev Flyway incluye classpath:db/dev y aplica V1 y V2 más la demo")
    void flywayConDbDev() {
        assertThat(entorno.getProperty("spring.flyway.locations")).contains("classpath:db/dev");
        assertThat(entorno.getProperty("server.port")).isEqualTo("8082");
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2') AND success", Integer.class);
        assertThat(aplicadas).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-05 el servicio enlaza su cola a las routing keys de los eventos que consume del AsyncAPI")
    void enlacesDeLaCola() {
        assertThat(entorno.getProperty("fitstore.eventos.servicio")).isEqualTo("catalog");
        assertThat(entorno.getProperty("fitstore.eventos.enlaces[0]")).isEqualTo("order.pedido.pagado");
        assertThat(entorno.getProperty("fitstore.eventos.enlaces[1]")).isEqualTo("order.pedido.cancelado");
        assertThat(entorno.getProperty("fitstore.eventos.enlaces[2]")).isEqualTo("order.pedido.expirado");
    }
}
