package com.fitstore.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.auth.pruebas.PostgresPrueba;
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
 * Flyway aplica V1 y tolera que {@code classpath:db/dev} todavía no exista (los datos de demostración los agrega cada spec).
 */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/auth-service.yml,file:../../infra/config-repo/auth-service-dev.yml",
    "fitstore.outbox.intervalo-ms=3600000",
    "fitstore.auth.issuer=http://localhost:8080"
})
@ActiveProfiles({"sin-config-server", "dev"})
@ContextConfiguration(initializers = PostgresPrueba.class)
class ConfiguracionDevTest {

    @Autowired Environment entorno;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-05 con el perfil dev Flyway incluye classpath:db/dev y aplica V1 aunque db/dev no exista")
    void flywayConDbDev() {
        assertThat(entorno.getProperty("spring.flyway.locations")).contains("classpath:db/dev");
        assertThat(entorno.getProperty("server.port")).isEqualTo("8081");
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }
}
