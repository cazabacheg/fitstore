package com.fitstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.pruebas.EntornoGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fitstore.gateway.pruebas.PruebaGateway;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CA-0a-11 (parte 2): si Redis no está disponible, el gateway deja pasar y registra una advertencia (infraestructura 4.4). */
@PruebaGateway
@ExtendWith(OutputCaptureExtension.class)
class GatewayRedisCaidoTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro, 1); // nadie escucha en el puerto 1
    }

    @LocalServerPort
    int puerto;

    @Test
    @DisplayName("CA-0a-11 con Redis caído el gateway deja pasar y registra una advertencia")
    void redisCaidoDejaPasar(CapturedOutput salida) {
        WebTestClient cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto).build();

        for (int i = 1; i <= 8; i++) { // más que el límite de login (5): nadie debe recibir 429
            cliente.post().uri("/api/v1/auth/login").header("X-Forwarded-For", "203.0.113.200")
                    .bodyValue("{}").exchange().expectStatus().isOk();
        }

        assertThat(salida.getAll()).contains("WARN").contains("Redis no está disponible");
    }
}
