package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.settings.pruebas.PruebaDatos;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Textos modelo de la versión 1 de las cuatro políticas (SPEC-3, sección 6): reemplazan al texto de ejemplo de 0a,
 * están marcados como plantilla a revisar y nunca pisan lo que ya publicó una tienda.
 */
class PoliticasModeloTest extends PruebaDatos {

    private static final String MIGRACION = "db/migration/V20261020_1100__politicas_modelo.sql";

    @Autowired
    JdbcTemplate jdbc;

    private String contenidoV1(String tipo) {
        return jdbc.queryForObject("SELECT contenido FROM politica WHERE tipo = ? AND version = 1", String.class, tipo);
    }

    @Test
    @DisplayName("CA-3-21 la versión 1 de las cuatro políticas es un texto modelo, ya no el de ejemplo")
    void lasCuatroTienenTextoModelo() {
        for (String tipo : new String[] {"TERMINOS", "PRIVACIDAD", "CAMBIOS_DEVOLUCIONES", "ENVIOS"}) {
            assertThat(contenidoV1(tipo)).as(tipo).doesNotContain("contenido de ejemplo").hasSizeGreaterThan(1500);
        }
    }

    @Test
    @DisplayName("CA-3-21 cada texto modelo avisa que es una plantilla a revisar por la tienda y no asesoría legal")
    void marcadosComoPlantilla() {
        for (String tipo : new String[] {"TERMINOS", "PRIVACIDAD", "CAMBIOS_DEVOLUCIONES", "ENVIOS"}) {
            assertThat(contenidoV1(tipo)).as(tipo).contains("Plantilla a revisar por la tienda")
                    .contains("no constituye asesoría legal");
        }
    }

    @Test
    @DisplayName("CA-3-21 el modelo de privacidad cita la Ley 29733 y los derechos ARCO")
    void privacidadConLey29733() {
        assertThat(contenidoV1("PRIVACIDAD")).contains("Ley N.° 29733").contains("acceso").contains("rectificación")
                .contains("cancelación").contains("oposición");
    }

    @Test
    @DisplayName("CA-3-21 los modelos de términos y de cambios citan el Código de Protección y Defensa del Consumidor")
    void citanElCodigoDelConsumidor() {
        assertThat(contenidoV1("TERMINOS")).contains("Libro de Reclamaciones").contains("Ley N.° 29571");
        assertThat(contenidoV1("CAMBIOS_DEVOLUCIONES")).contains("Ley N.° 29571");
    }

    @Test
    @DisplayName("CA-3-21 los modelos de envíos y de cambios describen las modalidades y el plazo de la tienda")
    void enviosYCambiosDescriptivos() {
        assertThat(contenidoV1("ENVIOS")).contains("delivery").contains("agencia").contains("recojo");
        assertThat(contenidoV1("CAMBIOS_DEVOLUCIONES")).contains("30 días");
    }

    @Test
    @DisplayName("CA-3-21 la migración no pisa un texto que la tienda ya publicó como versión 1")
    void noPisaLoPublicado() throws IOException {
        jdbc.update("UPDATE politica SET contenido = 'Texto propio de la tienda' WHERE tipo = 'TERMINOS' AND version = 1");

        jdbc.execute(new ClassPathResource(MIGRACION).getContentAsString(StandardCharsets.UTF_8));

        assertThat(contenidoV1("TERMINOS")).isEqualTo("Texto propio de la tienda");
    }

    @Test
    @DisplayName("CA-3-21 la migración sí reemplaza el texto de ejemplo de 0a y no toca versiones posteriores")
    void reemplazaSoloElEjemplo() throws IOException {
        jdbc.update("UPDATE politica SET contenido = 'Este es contenido de ejemplo. Hola' "
                + "WHERE tipo = 'ENVIOS' AND version = 1");
        jdbc.update("INSERT INTO politica (tipo, version, contenido) VALUES ('ENVIOS', 2, 'Contenido de ejemplo versión 2')");

        jdbc.execute(new ClassPathResource(MIGRACION).getContentAsString(StandardCharsets.UTF_8));

        assertThat(contenidoV1("ENVIOS")).contains("Plantilla a revisar por la tienda");
        assertThat(jdbc.queryForObject("SELECT contenido FROM politica WHERE tipo = 'ENVIOS' AND version = 2",
                String.class)).isEqualTo("Contenido de ejemplo versión 2");
    }
}
