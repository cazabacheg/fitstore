package com.fitstore.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;

/** Infraestructura 3: Eureka standalone, que no se registra a sí mismo; en dev sin self-preservation. */
@SpringBootTest(classes = DiscoveryServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=dev")
class DiscoveryServerTest {

    @LocalServerPort
    int puerto;

    @Autowired
    Environment entorno;

    private HttpResponse<String> get(String ruta, String aceptar) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).header("Accept", aceptar).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Eureka responde el registro de aplicaciones vacío al arrancar")
    void registroDisponible() throws Exception {
        HttpResponse<String> respuesta = get("/eureka/apps", "application/json");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("applications");
    }

    @Test
    @DisplayName("/actuator/health responde UP")
    void salud() throws Exception {
        HttpResponse<String> respuesta = get("/actuator/health", "application/json");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("UP");
    }

    @Test
    @DisplayName("Standalone: no se registra a sí mismo ni trae el registro; en dev sin self-preservation")
    void configuracionStandalone() {
        assertThat(entorno.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(entorno.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
        assertThat(entorno.getProperty("eureka.server.enable-self-preservation", Boolean.class)).isFalse();
        assertThat(entorno.getProperty("server.port", Integer.class)).isNotNull();
    }
}
