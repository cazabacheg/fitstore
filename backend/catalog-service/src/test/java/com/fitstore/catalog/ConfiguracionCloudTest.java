package com.fitstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.catalog.pruebas.PostgresPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/** Con el perfil cloud solo se aplican las migraciones del esquema, sin datos de demostración. */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/catalog-service.yml,file:../../infra/config-repo/catalog-service-cloud.yml",
    "fitstore.outbox.habilitado=false",
    "fitstore.storage.habilitado=false"
})
@ActiveProfiles({"sin-config-server", "cloud"})
@ContextConfiguration(initializers = PostgresPrueba.class)
class ConfiguracionCloudTest {

    @Autowired Environment entorno;

    @Test
    @DisplayName("CA-0a-05 con el perfil cloud Flyway solo incluye classpath:db/migration")
    void flywaySinDbDev() {
        assertThat(entorno.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
    }
}
