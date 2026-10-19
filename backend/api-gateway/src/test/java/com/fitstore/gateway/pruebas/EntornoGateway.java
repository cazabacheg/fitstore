package com.fitstore.gateway.pruebas;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Piezas compartidas por las pruebas del gateway: servicios simulados, Redis real y la clave de prueba. */
public final class EntornoGateway {

    public static final ServiciosSimulados SERVICIOS = new ServiciosSimulados();
    public static final TokensDePrueba EMISOR = new TokensDePrueba("kid-gateway");
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8")).withExposedPorts(6379);

    static {
        REDIS.start();
    }

    private EntornoGateway() {
    }

    /**
     * Configura el gateway con sus rutas reales ({@code infra/config-repo/api-gateway.yml}), sin config-server ni
     * Eureka, con el descubrimiento simple apuntando a los WireMock, la clave de prueba y Redis.
     */
    public static void registrar(DynamicPropertyRegistry registro, int puertoRedis) {
        registro.add("spring.config.import", () -> "file:../../infra/config-repo/api-gateway.yml");
        registro.add("spring.cloud.config.enabled", () -> "false");
        registro.add("eureka.client.enabled", () -> "false");
        registro.add("spring.cloud.config.username", () -> "prueba");
        registro.add("spring.cloud.config.password", () -> "prueba");
        registro.add("fitstore.security.jwks-archivo", () -> EMISOR.escribirJwks().toString());
        registro.add("spring.data.redis.host", () -> "localhost");
        registro.add("spring.data.redis.port", () -> puertoRedis);
        // Sin exportar trazas salvo en la prueba que lo verifica
        registro.add("management.tracing.export.enabled", () -> "false");
        SERVICIOS.registrarEn(registro);
    }

    public static void registrar(DynamicPropertyRegistry registro) {
        registrar(registro, REDIS.getMappedPort(6379));
    }
}
