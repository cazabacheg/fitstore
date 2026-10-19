package com.fitstore.configserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/** CA-0a-04: configuración centralizada. Un cliente con perfil dev toma su configuración del config-server. */
@SpringBootTest(
        classes = ConfigServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.config.server.native.search-locations=file:src/test/resources/config-repo-prueba",
            "spring.security.user.name=config-prueba",
            "spring.security.user.password=clave-de-prueba"
        })
class ConfigServerTest {

    private static final String USUARIO = "config-prueba";
    private static final String CLAVE = "clave-de-prueba";

    @LocalServerPort
    int puerto;

    /** Aplicación cliente mínima, sin web, que arranca con spring.config.import=configserver:. */
    @Configuration
    @EnableAutoConfiguration(exclude = org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class)
    static class ClienteDePrueba {
    }

    /** Argumentos de línea de comandos (máxima prioridad) para que el application.yml del propio módulo no los pise. */
    private static String[] argumentosCliente(String urlConfigServer, String... extra) {
        java.util.List<String> args = new java.util.ArrayList<>(java.util.List.of(
                "--spring.application.name=servicio-prueba",
                "--spring.profiles.active=dev",
                "--spring.config.import=configserver:" + urlConfigServer,
                "--CONFIG_USER=" + USUARIO, "--CONFIG_PASSWORD=" + CLAVE, // el application.yml del módulo los exige
                "--spring.cloud.config.enabled=true", // el jar del config-server la desactiva por defecto en este classpath
                "--spring.cloud.config.username=" + USUARIO,
                "--spring.cloud.config.password=" + CLAVE));
        args.addAll(java.util.List.of(extra));
        return args.toArray(String[]::new);
    }

    private static ConfigurableApplicationContext arrancarCliente(String urlConfigServer, String... extra) {
        return new SpringApplicationBuilder(ClienteDePrueba.class)
                .web(WebApplicationType.NONE)
                .run(argumentosCliente(urlConfigServer, extra));
    }

    @Test
    @DisplayName("CA-0a-04 un servicio con perfil dev toma su configuración del config-server")
    void tomaLaConfiguracionDelConfigServer() {
        try (ConfigurableApplicationContext contexto = arrancarCliente("http://localhost:" + puerto)) {
            assertThat(contexto.getEnvironment().getProperty("prueba.mensaje")).isEqualTo("valor-de-dev");
            assertThat(contexto.getEnvironment().getProperty("prueba.solo-comun")).isEqualTo("presente");
        }
    }

    @Test
    @DisplayName("CA-0a-04 el config-server exige usuario y contraseña, salvo en /actuator/health")
    void exigeAutenticacion() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        var sinCredenciales = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/servicio-prueba/dev")).build(),
                HttpResponse.BodyHandlers.ofString());
        var conCredenciales = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/servicio-prueba/dev"))
                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((USUARIO + ":" + CLAVE).getBytes()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        var salud = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(sinCredenciales.statusCode()).isEqualTo(401);
        assertThat(conCredenciales.statusCode()).isEqualTo(200);
        assertThat(conCredenciales.body()).contains("valor-de-dev");
        assertThat(salud.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("CA-0a-04 si el config-server no responde, el servicio reintenta y no arranca con valores por defecto")
    void reintentaYNoArrancaSinConfigServer() throws Exception {
        AtomicInteger peticiones = new AtomicInteger();
        HttpServer caido = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        caido.createContext("/", intercambio -> {
            peticiones.incrementAndGet();
            intercambio.sendResponseHeaders(503, -1);
            intercambio.close();
        });
        caido.start();
        try {
            String url = "http://127.0.0.1:" + caido.getAddress().getPort();

            assertThatThrownBy(() -> arrancarCliente(url,
                    "--spring.cloud.config.fail-fast=true",
                    "--spring.cloud.config.retry.max-attempts=3",
                    "--spring.cloud.config.retry.initial-interval=50",
                    "--spring.cloud.config.retry.multiplier=1.5")).isInstanceOf(Exception.class);
            assertThat(peticiones.get()).isGreaterThanOrEqualTo(3);
        } finally {
            caido.stop(0);
        }
    }
}
