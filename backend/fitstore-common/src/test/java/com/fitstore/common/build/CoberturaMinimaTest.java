package com.fitstore.common.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CA-0a-21: el POM padre exige el 80 % de líneas por módulo con las exclusiones de SPEC-0a I7. Esta prueba protege
 * la configuración; que el build falle de verdad se demostró bajando el umbral de un módulo real
 * ({@code ./mvnw -pl backend/api-gateway verify -Djacoco.umbral=0.99} → "Rule violated for bundle api-gateway").
 */
class CoberturaMinimaTest {

    private final String pom = leerPomPadre();

    private static String leerPomPadre() {
        try {
            return Files.readString(Path.of("..", "..", "pom.xml"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el POM padre", e);
        }
    }

    @Test
    @DisplayName("CA-0a-21 el POM padre usa JaCoCo 0.8.15 con informe y verificación en verify")
    void jacocoEnVerify() {
        assertThat(pom).contains("<jacoco.version>0.8.15</jacoco.version>");
        assertThat(pom).contains("<goal>report</goal>").contains("<goal>check</goal>");
        assertThat(pom.split("<phase>verify</phase>", -1).length - 1).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-21 el mínimo es 0,80 de líneas cubiertas por módulo")
    void minimoDeLineas() {
        assertThat(pom).contains("<jacoco.umbral>0.80</jacoco.umbral>");
        assertThat(pom).contains("<element>BUNDLE</element>")
                .contains("<counter>LINE</counter>")
                .contains("<value>COVEREDRATIO</value>")
                .contains("<minimum>${jacoco.umbral}</minimum>");
    }

    @Test
    @DisplayName("CA-0a-21 se excluyen configuración, DTOs y clases Application (I7)")
    void exclusiones() {
        assertThat(pom).contains("<exclude>**/config/**</exclude>")
                .contains("<exclude>**/dto/**</exclude>")
                .contains("<exclude>**/*Application.class</exclude>");
    }
}
