package com.fitstore.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** CA-1-37 (A5): el JWKS de prueba de la 0a ya no existe y nadie en {@code infra/} lo usa. Lee el repositorio. */
class RetiroJwksPruebaTest {

    private static final Path RAIZ = Path.of("../..");
    private static final Path INFRA = RAIZ.resolve("infra");

    @Test
    @DisplayName("CA-1-37 el JWKS de prueba y su clave ya no existen en infra/dev")
    void archivosEliminados() {
        assertThat(INFRA.resolve("dev/jwks-prueba.json")).doesNotExist();
        assertThat(INFRA.resolve("dev/clave-privada-PRUEBA-solo-dev.jwk.json")).doesNotExist();
    }

    @Test
    @DisplayName("CA-1-37 nada en infra/ usa jwks-archivo ni FITSTORE_SECURITY_JWKS_ARCHIVO")
    void nadieLoUsa() throws IOException {
        assertThat(INFRA).isDirectory();
        try (Stream<Path> archivos = Files.walk(INFRA)) {
            List<Path> usan = archivos.filter(Files::isRegularFile).filter(RetiroJwksPruebaTest::esTexto)
                    .filter(a -> {
                        String contenido = leer(a).toLowerCase();
                        return contenido.contains("jwks-archivo") || contenido.contains("fitstore_security_jwks_archivo")
                                || contenido.contains("jwks-prueba");
                    }).toList();
            assertThat(usan).isEmpty();
        }
    }

    @Test
    @DisplayName("CA-1-37 todos los servicios validan con el JWKS de auth (jwk-set-uri en application.yml del config-repo)")
    @SuppressWarnings("unchecked")
    void jwkSetUriDeAuth() throws IOException {
        String yaml = Files.readString(INFRA.resolve("config-repo/application.yml"));
        Map<String, Object> raiz = null;
        for (Object documento : new Yaml().loadAll(yaml)) {
            Map<String, Object> actual = (Map<String, Object>) documento;
            if (actual != null && actual.containsKey("spring") && ((Map<String, Object>) actual.get("spring")).containsKey("security")) {
                raiz = actual;
            }
        }
        assertThat(raiz).as("un documento del application.yml configura spring.security").isNotNull();
        Map<String, Object> spring = (Map<String, Object>) raiz.get("spring");
        Map<String, Object> seguridad = (Map<String, Object>) spring.get("security");
        Map<String, Object> oauth = (Map<String, Object>) seguridad.get("oauth2");
        Map<String, Object> recurso = (Map<String, Object>) oauth.get("resourceserver");
        Map<String, Object> jwt = (Map<String, Object>) recurso.get("jwt");
        assertThat(jwt.get("jwk-set-uri")).isEqualTo("http://auth-service:8081/.well-known/jwks.json");
    }

    private static boolean esTexto(Path archivo) {
        try {
            return Files.size(archivo) < 1_000_000 && !Files.probeContentType(archivo).startsWith("image");
        } catch (IOException | NullPointerException e) {
            return true;
        }
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            return "";
        }
    }
}
