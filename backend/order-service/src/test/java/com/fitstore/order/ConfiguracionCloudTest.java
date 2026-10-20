package com.fitstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.pruebas.EntornoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/** Con el perfil cloud Flyway solo aplica las migraciones: nunca los datos de demostración (CA-0a-05). */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/order-service.yml,file:../../infra/config-repo/order-service-cloud.yml",
    "fitstore.outbox.habilitado=false"
})
@ActiveProfiles({"sin-config-server", "cloud"})
@ContextConfiguration(initializers = EntornoPrueba.class)
class ConfiguracionCloudTest {

    @Autowired Environment entorno;

    @Test
    @DisplayName("CA-0a-05 con el perfil cloud Flyway usa solo classpath:db/migration")
    void flywaySinDatosDeDemostracion() {
        assertThat(entorno.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
    }
}
