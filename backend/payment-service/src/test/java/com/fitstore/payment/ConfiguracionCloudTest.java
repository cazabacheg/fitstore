package com.fitstore.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.pruebas.PostgresPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * El servicio arranca con los YAML reales de {@code infra/config-repo} y el perfil cloud: sin datos de demostración.
 */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/payment-service.yml,file:../../infra/config-repo/payment-service-cloud.yml",
    "fitstore.outbox.intervalo-ms=3600000",
    "S3_ENDPOINT=http://localhost:3900",
    "S3_ACCESS_KEY=clave-de-prueba",
    "S3_SECRET_KEY=secreto-de-prueba"
})
@ActiveProfiles({"sin-config-server", "cloud"})
@ContextConfiguration(initializers = PostgresPrueba.class)
class ConfiguracionCloudTest {

    @Autowired Environment entorno;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-05 con el perfil cloud Flyway aplica solo db/migration y la pasarela simulada queda apagada")
    void flywayEnCloud() {
        assertThat(entorno.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
        assertThat(entorno.getProperty("server.port")).isEqualTo("8084");
        assertThat(entorno.getProperty("fitstore.pagos.simulado.habilitado")).isEqualTo("false");
        assertThat(entorno.getProperty("fitstore.storage.bucket-privado")).isEqualTo("fitstore-privado");
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }
}
