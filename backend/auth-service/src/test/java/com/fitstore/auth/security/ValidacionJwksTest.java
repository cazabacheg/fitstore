package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PostgresAislada;
import com.fitstore.auth.pruebas.RedisPrueba;
import com.fitstore.auth.repository.StaffRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CA-1-36: un token emitido por auth se valida con un decodificador que lee {@code /.well-known/jwks.json} del servicio
 * levantado (como hacen el gateway y los demás servicios) y se respetan {@code rol} y {@code tipo}. El recorrido por el
 * gateway real lo cubre la colección Postman.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = {PostgresAislada.class, RedisPrueba.class})
class ValidacionJwksTest {

    @LocalServerPort int puerto;
    @Autowired StaffRepository personal;
    @Autowired JsonMapper json;

    private final HttpClient http = HttpClient.newHttpClient();

    private String url(String ruta) {
        return "http://localhost:" + puerto + ruta;
    }

    private HttpResponse<String> enviar(HttpRequest peticion) throws Exception {
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private String tokenDeUnAdmin() throws Exception {
        String email = "admin." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@fitstore.pe";
        Staff admin = Datos.staff(email, RolStaff.ADMIN);
        admin.setPasswordHash(new BCryptPasswordEncoder(4).encode("Gamarra2026"));
        personal.save(admin);
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(URI.create(url("/api/v1/auth/staff/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"Gamarra2026\"}")).build());
        assertThat(r.statusCode()).isEqualTo(200);
        return json.readTree(r.body()).path("data").path("accessToken").asString();
    }

    @Test
    @DisplayName("CA-1-36 el JWKS es público, publica solo la clave pública con kid y se puede cachear")
    void jwksPublico() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(URI.create(url("/.well-known/jwks.json"))).GET().build());

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Cache-Control")).hasValue("public, max-age=3600");
        JsonNode clave = json.readTree(r.body()).path("keys").get(0);
        assertThat(clave.path("kty").asString()).isEqualTo("RSA");
        assertThat(clave.path("kid").asString()).isEqualTo("fs-prueba");
        assertThat(clave.path("use").asString()).isEqualTo("sig");
        assertThat(clave.path("alg").asString()).isEqualTo("RS256");
        assertThat(clave.path("n").asString()).isNotBlank();
        assertThat(clave.path("e").asString()).isNotBlank();
        for (String privado : new String[] {"d", "p", "q", "dp", "dq", "qi"}) {
            assertThat(clave.has(privado)).as("la clave privada no se publica (%s)", privado).isFalse();
        }
    }

    @Test
    @DisplayName("CA-1-36 un token real de auth se valida con el JWKS del servicio y conserva rol y tipo")
    void tokenRealSeValidaConElJwks() throws Exception {
        String token = tokenDeUnAdmin();
        NimbusJwtDecoder otroServicio = NimbusJwtDecoder.withJwkSetUri(url("/.well-known/jwks.json")).build();

        Jwt jwt = otroServicio.decode(token);

        assertThat(jwt.<String>getClaim("rol")).isEqualTo("ADMIN");
        assertThat(jwt.<String>getClaim("tipo")).isEqualTo("STAFF");
        assertThat(jwt.getSubject()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("kid", "fs-prueba");
    }

    @Test
    @DisplayName("CA-1-36 un token adulterado no pasa la validación")
    void tokenAdulterado() throws Exception {
        String token = tokenDeUnAdmin();
        String[] partes = token.split("\\.");
        // cambia el rol dentro del payload sin volver a firmar
        String payload = new String(java.util.Base64.getUrlDecoder().decode(partes[1])).replace("\"ADMIN\"", "\"OPERADOR\"");
        String adulterado = partes[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes())
                + "." + partes[2];
        NimbusJwtDecoder otroServicio = NimbusJwtDecoder.withJwkSetUri(url("/.well-known/jwks.json")).build();

        assertThatThrownBy(() -> otroServicio.decode(adulterado)).isInstanceOf(JwtException.class);
    }
}
