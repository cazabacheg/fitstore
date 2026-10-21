package com.fitstore.settings.configuracion;

import static com.fitstore.settings.pruebas.Sesiones.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.configuracion.service.CachePublica;
import com.fitstore.settings.pruebas.PruebaApi;
import com.fitstore.settings.pruebas.RestauraSemilla;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * La configuración pública se sirve desde Redis y se invalida al guardar (CA-3-35). Estas pruebas confirman de
 * verdad (no usan la transacción de la prueba) para que la invalidación posterior a la confirmación también corra.
 */
class CachePublicaTest extends PruebaApi {

    @AfterEach
    void restaurarLaSemilla() {
        RestauraSemilla.ejecutar(jdbc);
    }

    private int versionActual() throws Exception {
        return leer(mvc.perform(get("/api/v1/admin/settings").with(admin())).andReturn(), "$.data.version");
    }

    private void pedirLoPublico() throws Exception {
        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-3-35 la primera lectura llena la caché de Redis con vigencia de hasta 1 hora")
    void laPrimeraLecturaLlenaLaCache() throws Exception {
        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();

        pedirLoPublico();

        assertThat(redis.hasKey(CachePublica.CLAVE)).isTrue();
        assertThat(redis.getExpire(CachePublica.CLAVE)).isBetween(3000L, 3600L);
        assertThat(redis.opsForValue().get(CachePublica.CLAVE)).contains("\"nombre\":\"FitStore\"")
                .doesNotContain("correosAlertas");
    }

    @Test
    @DisplayName("CA-3-35 /settings/public responde desde la caché: un cambio hecho por fuera de la API no se ve hasta invalidar")
    void respondeDesdeLaCache() throws Exception {
        pedirLoPublico();
        jdbc.update("UPDATE configuracion_tienda SET nombre = 'Cambiado a mano' WHERE id = 1");

        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.nombre", is("FitStore")));

        redis.delete(CachePublica.CLAVE);
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.identidad.nombre", is("Cambiado a mano")));
    }

    @Test
    @DisplayName("CA-3-35 guardar cualquiera de las cuatro secciones invalida la caché y la tienda ve el cambio")
    void guardarUnaSeccionInvalidaLaCache() throws Exception {
        Map<String, Object> identidad = new LinkedHashMap<>();
        identidad.put("nombre", "Tienda Nueva");
        identidad.put("razonSocial", "Tienda Nueva S.A.C.");
        identidad.put("ruc", "20123456789");
        identidad.put("direccionFiscal", "Jr. Gamarra 700, La Victoria");
        identidad.put("version", versionActual());
        pedirLoPublico();
        mvc.perform(put("/api/v1/admin/settings/identidad").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(identidad))).andExpect(status().isOk());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.nombre", is("Tienda Nueva")));

        Map<String, Object> tema = new LinkedHashMap<>();
        tema.put("colorPrimario", "#1E3A8A");
        tema.put("colorSecundario", "#2B2621");
        tema.put("colorAcento", "#4E5B45");
        tema.put("colorFondo", "#FFFFFF");
        tema.put("colorTexto", "#111111");
        tema.put("version", versionActual());
        mvc.perform(put("/api/v1/admin/settings/tema").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(tema))).andExpect(status().isOk());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.tema.colorPrimario", is("#1E3A8A")));

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("heroTitulo", "Título nuevo");
        contenido.put("beneficios", List.of());
        contenido.put("version", versionActual());
        mvc.perform(put("/api/v1/admin/settings/contenido").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(contenido))).andExpect(status().isOk());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.contenido.heroTitulo", is("Título nuevo")));

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("metodosPago", List.of("YAPE"));
        parametros.put("modalidadesEntrega", List.of("RECOJO"));
        parametros.put("expiracionPedidoMinutos", 60);
        parametros.put("diasReembolsoPostventa", 30);
        parametros.put("correosAlertas", List.of("ventas@tienda.pe"));
        parametros.put("version", versionActual());
        mvc.perform(put("/api/v1/admin/settings/parametros").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(parametros))).andExpect(status().isOk());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.parametros.expiracionPedidoMinutos", is(60)));
    }

    @Test
    @DisplayName("CA-3-35 subir una imagen también invalida la caché")
    void subirUnaImagenInvalidaLaCache() throws Exception {
        pedirLoPublico();
        assertThat(redis.hasKey(CachePublica.CLAVE)).isTrue();

        byte[] png = java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/test/resources/imagenes/icono.ico"));
        mvc.perform(multipart("/api/v1/admin/settings/imagenes")
                .file(new MockMultipartFile("archivo", "favicon.ico", "image/x-icon", png)).param("tipo", "FAVICON")
                .with(admin())).andExpect(status().isCreated());

        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.faviconUrl").isNotEmpty());
    }

    @Test
    @DisplayName("CA-3-35 un guardado rechazado (409 o 400) no toca la caché")
    void unGuardadoRechazadoNoInvalida() throws Exception {
        pedirLoPublico();
        Map<String, Object> mala = new LinkedHashMap<>();
        mala.put("nombre", "X");
        mala.put("razonSocial", "Y");
        mala.put("ruc", "123");
        mala.put("direccionFiscal", "Jr. Gamarra");
        mala.put("version", 0);
        mvc.perform(put("/api/v1/admin/settings/identidad").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(mala))).andExpect(status().isBadRequest());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isTrue();
    }

    @Test
    @DisplayName("CA-3-35 la caché expira sola con el TTL configurado")
    void laCacheTieneTtl() throws Exception {
        pedirLoPublico();
        Long segundos = redis.getExpire(CachePublica.CLAVE);
        assertThat(Duration.ofSeconds(segundos)).isLessThanOrEqualTo(Duration.ofHours(1));
    }
}
