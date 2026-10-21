package com.fitstore.settings.configuracion;

import static com.fitstore.settings.pruebas.Sesiones.ID_ADMIN;
import static com.fitstore.settings.pruebas.Sesiones.admin;
import static com.fitstore.settings.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.configuracion.service.CachePublica;
import com.fitstore.settings.pruebas.PruebaApi;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Políticas versionadas e inmutables (CA-3-20 a CA-3-22). */
@Transactional
class PoliticaServiceTest extends PruebaApi {

    private static final String TEXTO_NUEVO = "# Términos y condiciones\n\nVersión nueva de los términos de la tienda, "
            + "con más de cincuenta caracteres para que sea válida.";

    private ResultActions publicar(String tipo, String contenido) throws Exception {
        return mvc.perform(post("/api/v1/admin/settings/politicas/" + tipo).with(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(Map.of("contenido", contenido))));
    }

    @Test
    @DisplayName("CA-3-21 un visitante lee la versión vigente con su número y fecha, sin sesión")
    void leeLaVigente() throws Exception {
        mvc.perform(get("/api/v1/settings/politicas/terminos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo", is("terminos")))
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.contenido", containsString("Términos")))
                .andExpect(jsonPath("$.data.publicadaEn").isNotEmpty());
    }

    @Test
    @DisplayName("CA-3-21 las cuatro políticas existen con el tipo en el formato del contrato")
    void lasCuatroPoliticas() throws Exception {
        for (String tipo : new String[] {"terminos", "privacidad", "cambios-devoluciones", "envios"}) {
            mvc.perform(get("/api/v1/settings/politicas/" + tipo)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tipo", is(tipo))).andExpect(jsonPath("$.data.version", is(1)));
        }
    }

    @Test
    @DisplayName("CA-3-21 un tipo desconocido o una versión que no existe responde 404")
    void noEncontrada() throws Exception {
        mvc.perform(get("/api/v1/settings/politicas/cookies")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/settings/politicas/terminos").param("version", "99")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/settings/politicas/terminos").param("version", "0")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-20 publicar crea la versión siguiente con fecha y autor, y pasa a ser la vigente")
    void publicaLaVersionSiguiente() throws Exception {
        publicar("terminos", TEXTO_NUEVO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(2)))
                .andExpect(jsonPath("$.data.tipo", is("terminos")))
                .andExpect(jsonPath("$.data.contenido", is(TEXTO_NUEVO)));

        mvc.perform(get("/api/v1/settings/politicas/terminos")).andExpect(jsonPath("$.data.version", is(2)))
                .andExpect(jsonPath("$.data.contenido", is(TEXTO_NUEVO)));
        assertThat(jdbc.queryForObject(
                "SELECT publicada_por FROM politica WHERE tipo = 'TERMINOS' AND version = 2", String.class))
                .isEqualTo(ID_ADMIN);
    }

    @Test
    @DisplayName("CA-3-20 las versiones anteriores se conservan intactas y se leen con ?version=N (CA-3-21)")
    void conservaLasAnteriores() throws Exception {
        String original = leer(mvc.perform(get("/api/v1/settings/politicas/terminos")).andReturn(), "$.data.contenido");
        publicar("terminos", TEXTO_NUEVO).andExpect(status().isCreated());
        publicar("terminos", TEXTO_NUEVO + " Otra vez.").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version", is(3)));

        mvc.perform(get("/api/v1/settings/politicas/terminos").param("version", "1"))
                .andExpect(jsonPath("$.data.version", is(1))).andExpect(jsonPath("$.data.contenido", is(original)));
        mvc.perform(get("/api/v1/settings/politicas/terminos").param("version", "2"))
                .andExpect(jsonPath("$.data.contenido", is(TEXTO_NUEVO)));
    }

    @Test
    @DisplayName("CA-3-20 no hay forma de editar ni eliminar una versión: solo se publica la siguiente")
    void soloSePuedePublicar() throws Exception {
        String ruta = "/api/v1/admin/settings/politicas/terminos";
        mvc.perform(put(ruta).with(admin()).contentType(APPLICATION_JSON).content(cuerpo(Map.of("contenido", TEXTO_NUEVO))))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(ruta).with(admin())).andExpect(status().isMethodNotAllowed());
        mvc.perform(put(ruta + "/versiones").with(admin())).andExpect(status().isMethodNotAllowed());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM politica WHERE tipo = 'TERMINOS'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-3-20 el historial muestra la más reciente primero, con su autor y cuál es la vigente")
    void historialDeVersiones() throws Exception {
        publicar("privacidad", TEXTO_NUEVO).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/admin/settings/politicas/privacidad/versiones").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].version", is(2))).andExpect(jsonPath("$.data[0].vigente", is(true)))
                .andExpect(jsonPath("$.data[0].publicadaPor", is(ID_ADMIN)))
                .andExpect(jsonPath("$.data[1].version", is(1))).andExpect(jsonPath("$.data[1].vigente", is(false)))
                .andExpect(jsonPath("$.data[0].contenido").doesNotExist());
    }

    @Test
    @DisplayName("CA-3-20 el contenido va de 50 a 50 000 caracteres y solo el ADMIN publica")
    void validacionesYPermisos() throws Exception {
        publicar("terminos", "muy corto").andExpect(status().isBadRequest());
        publicar("terminos", "x".repeat(50_001)).andExpect(status().isBadRequest());
        publicar("cookies", TEXTO_NUEVO).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/admin/settings/politicas/terminos").with(operador()).contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("contenido", TEXTO_NUEVO)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/settings/politicas/terminos/versiones").with(operador()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/settings/politicas/terminos").contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("contenido", TEXTO_NUEVO)))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-3-21 el backend guarda el Markdown tal cual: no lo transforma ni lo renderiza")
    void guardaElMarkdownTalCual() throws Exception {
        String peligroso = "# Aviso\n\n<script>alert(1)</script> y <img src=x onerror=alert(2)> [clic](javascript:alert(3)) "
                + "texto de relleno para superar los cincuenta caracteres";
        publicar("envios", peligroso).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/settings/politicas/envios")).andExpect(jsonPath("$.data.contenido", is(peligroso)));
    }

    @Test
    @DisplayName("CA-3-22 /settings/public expone la versión vigente de cada política")
    void lasVersionesEnLoPublico() throws Exception {
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.politicas.terminos", is(1)))
                .andExpect(jsonPath("$.data.politicas.privacidad", is(1)))
                .andExpect(jsonPath("$.data.politicas.cambiosDevoluciones", is(1)))
                .andExpect(jsonPath("$.data.politicas.envios", is(1)));

        publicar("terminos", TEXTO_NUEVO).andExpect(status().isCreated());
        publicar("cambios-devoluciones", TEXTO_NUEVO).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.politicas.terminos", is(2)))
                .andExpect(jsonPath("$.data.politicas.privacidad", is(1)))
                .andExpect(jsonPath("$.data.politicas.cambiosDevoluciones", is(2)))
                .andExpect(jsonPath("$.data.politicas.envios", not(2)));
    }

    @Test
    @DisplayName("CA-3-22 al publicar una versión se invalida la caché de /settings/public")
    void publicarInvalidaLaCache() throws Exception {
        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk());
        assertThat(redis.hasKey(CachePublica.CLAVE)).isTrue();

        publicar("terminos", TEXTO_NUEVO).andExpect(status().isCreated());

        assertThat(redis.hasKey(CachePublica.CLAVE)).isFalse();
    }
}
