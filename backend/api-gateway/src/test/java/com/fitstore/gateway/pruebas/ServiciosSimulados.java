package com.fitstore.gateway.pruebas;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Los 7 servicios de negocio simulados con WireMock; cada uno responde con su propio nombre para saber a quién llegó la petición. */
public final class ServiciosSimulados {

    public static final List<String> NOMBRES = List.of("auth-service", "catalog-service", "order-service",
            "payment-service", "delivery-service", "notification-service", "settings-service");

    private final Map<String, WireMockServer> servidores = new LinkedHashMap<>();

    public ServiciosSimulados() {
        for (String nombre : NOMBRES) {
            WireMockServer servidor = new WireMockServer(WireMockConfiguration.options().port(puertoLibre()));
            servidor.start();
            servidor.stubFor(any(anyUrl()).willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"success\":true,\"mensaje\":\"Operación exitosa\",\"data\":{\"servicio\":\"" + nombre + "\"}}")));
            servidores.put(nombre, servidor);
        }
    }

    /** Puerto fijo para poder detener y volver a levantar un servicio simulado en la misma dirección. */
    private static int puertoLibre() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public WireMockServer de(String servicio) {
        return servidores.get(servicio);
    }

    public void reiniciarPeticiones() {
        servidores.values().forEach(WireMockServer::resetRequests);
    }

    /** Registra cada servicio en el descubrimiento simple de Spring Cloud (sin Eureka) apuntando a su WireMock. */
    public void registrarEn(DynamicPropertyRegistry registro) {
        servidores.forEach((nombre, servidor) -> registro.add(
                "spring.cloud.discovery.client.simple.instances." + nombre + "[0].uri",
                () -> "http://localhost:" + servidor.port()));
    }
}
