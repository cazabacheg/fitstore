package com.fitstore.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/** CA-0a-05: los datos de demostración ({@code classpath:db/dev}) solo se incluyen con los perfiles dev o demo. */
class ConfigRepoSettingsTest {

    private static final Path CONFIG_REPO = Path.of("../../infra/config-repo");

    private Properties cargar(String archivo) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(CONFIG_REPO.resolve(archivo)));
        return yaml.getObject();
    }

    @Test
    @DisplayName("CA-0a-05 el perfil dev incluye db/dev además de db/migration")
    void devIncluyeDatosDeDemostracion() {
        assertThat(cargar("settings-service-dev.yml").getProperty("spring.flyway.locations"))
                .contains("classpath:db/migration").contains("classpath:db/dev");
    }

    @Test
    @DisplayName("CA-0a-05 el perfil cloud no incluye db/dev (el perfil demo lo agrega la spec 8)")
    void cloudNoIncluyeDatosDeDemostracion() {
        Properties cloud = cargar("settings-service-cloud.yml");

        assertThat(cloud.getProperty("spring.flyway.locations", "classpath:db/migration")).doesNotContain("db/dev");
    }

    @Test
    @DisplayName("CA-0a-05 la configuración común no incluye db/dev")
    void baseNoIncluyeDatosDeDemostracion() {
        assertThat(cargar("settings-service.yml").getProperty("spring.flyway.locations", "")).doesNotContain("db/dev");
    }
}
