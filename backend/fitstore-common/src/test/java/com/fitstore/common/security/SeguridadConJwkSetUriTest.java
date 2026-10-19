package com.fitstore.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sin la propiedad del archivo, el resource server valida con jwk-set-uri (el JWKS de auth). */
@WebMvcTest(controllers = SeguridadConJwkSetUriTest.Controlador.class)
@ImportAutoConfiguration(FitstoreSecurityAutoConfiguration.class)
@Import(SeguridadConJwkSetUriTest.Controlador.class)
class SeguridadConJwkSetUriTest {

    static final TokensDePrueba EMISOR = new TokensDePrueba("kid-remoto");
    static HttpServer servidorJwks;

    @SpringBootConfiguration
    static class Aplicacion { }

    @RestController
    static class Controlador {
        @GetMapping("/api/v1/protegido")
        String protegido() { return "ok"; }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        servidorJwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorJwks.createContext("/.well-known/jwks.json", intercambio -> {
            byte[] cuerpo = EMISOR.jwksPublico().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidorJwks.start();
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + servidorJwks.getAddress().getPort() + "/.well-known/jwks.json");
    }

    @AfterAll
    static void detener() {
        servidorJwks.stop(0);
    }

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("I2 con jwk-set-uri un token válido de auth entra y uno sin token no")
    void validaContraElJwksRemoto() throws Exception {
        String token = EMISOR.token("7", "CLIENTE", "CLIENTE", Duration.ofMinutes(15));

        mvc.perform(get("/api/v1/protegido").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/protegido")).andExpect(status().isUnauthorized());
    }
}
