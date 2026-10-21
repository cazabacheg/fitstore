package com.fitstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.common.event.EventosProperties;
import com.fitstore.order.config.PropiedadesOrder;
import com.fitstore.order.pruebas.EntornoDemo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.yaml.snakeyaml.Yaml;

/**
 * El servicio arranca con los YAML reales de {@code infra/config-repo} y el perfil dev: Flyway aplica V1 y
 * tolera que {@code classpath:db/dev} todavía no exista (los datos de demostración los agrega cada spec).
 */
@SpringBootTest(properties = {
    "spring.config.import=file:../../infra/config-repo/order-service.yml,file:../../infra/config-repo/order-service-dev.yml",
    "fitstore.outbox.habilitado=true",
    "fitstore.outbox.intervalo-ms=3600000"
})
@ActiveProfiles({"sin-config-server", "dev"})
@ContextConfiguration(initializers = EntornoDemo.class)
class ConfiguracionDevTest {

    private static final Path ASYNCAPI = Path.of("../../contracts/asyncapi/fitstore-eventos.yaml");

    @Autowired Environment entorno;
    @Autowired JdbcTemplate jdbc;
    @Autowired EventosProperties eventos;
    @Autowired PropiedadesOrder propiedades;

    @Test
    @DisplayName("CA-0a-05 con el perfil dev Flyway incluye classpath:db/dev y aplica V1 y los datos de demostración de db/dev")
    void flywayConDbDev() {
        assertThat(entorno.getProperty("spring.flyway.locations")).contains("classpath:db/dev");
        assertThat(entorno.getProperty("server.port")).isEqualTo("8083");
        assertThat(entorno.getProperty("springdoc.api-docs.version")).isEqualTo("openapi_3_0");
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 la cola order.eventos se enlaza solo a routing keys que existen como canales del AsyncAPI")
    @SuppressWarnings("unchecked")
    void enlacesCoincidenConElAsyncApi() throws Exception {
        Map<String, Object> raiz = new Yaml().load(Files.readString(ASYNCAPI));
        Map<String, Map<String, Object>> canales = (Map<String, Map<String, Object>>) raiz.get("channels");
        Set<String> direcciones = canales.values().stream()
                .map(c -> (String) c.get("address")).collect(Collectors.toSet());

        assertThat(eventos.servicio()).isEqualTo("order");
        assertThat(eventos.cola()).isEqualTo("order.eventos");
        assertThat(eventos.enlaces()).containsExactlyInAnyOrder("payment.pago.confirmado", "payment.pago.rechazado",
                "payment.boleta.emitida", "payment.reembolso.completado", "delivery.entrega.actualizada");
        assertThat(direcciones).containsAll(eventos.enlaces());
    }

    @Test
    @DisplayName("CA-0a-05 las propiedades fitstore.order del micro 10.5 se leen del config-repo")
    void propiedadesDeOrder() {
        assertThat(propiedades.carrito().maxLineas()).isEqualTo(50);
        assertThat(propiedades.carrito().maxUnidadesLinea()).isEqualTo(100);
        assertThat(propiedades.carrito().ttlAnonimo()).isEqualTo(Duration.ofDays(7));
        assertThat(propiedades.carrito().ttlCliente()).isEqualTo(Duration.ofDays(30));
        assertThat(propiedades.codigoPrefijo()).isEqualTo("FS");
        assertThat(propiedades.settingsCacheTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(propiedades.expiradorCron()).isEqualTo("0 * * * * *");
        assertThat(propiedades.defaults().expiracionPedidoMinutos()).isEqualTo(30);
    }
}
