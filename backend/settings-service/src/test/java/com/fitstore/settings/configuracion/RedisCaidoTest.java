package com.fitstore.settings.configuracion;

import static com.fitstore.settings.pruebas.Sesiones.admin;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.pruebas.PruebaApiBase;
import com.fitstore.settings.pruebas.RestauraSemilla;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Con Redis caído la configuración se lee de la base de datos y guardar sigue funcionando (CA-3-37). */
class RedisCaidoTest extends PruebaApiBase {

    /** Un puerto donde no escucha nadie: Redis "caído". */
    @DynamicPropertySource
    static void redisCaido(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", () -> "127.0.0.1");
        registro.add("spring.data.redis.port", () -> 1);
    }

    @AfterEach
    void restaurarLaSemilla() {
        RestauraSemilla.ejecutar(jdbc);
    }

    @Test
    @DisplayName("CA-3-37 con Redis caído /settings/public se lee de la base de datos")
    void leeDeLaBase() throws Exception {
        assertThatThrownBy(() -> redis.hasKey("settings:public")).isInstanceOf(RuntimeException.class);
        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identidad.nombre", is("FitStore")))
                .andExpect(jsonPath("$.data.tema.colorPrimario", is("#C9B28C")));
        jdbc.update("UPDATE configuracion_tienda SET nombre = 'Desde la base' WHERE id = 1");
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.identidad.nombre", is("Desde la base")));
    }

    @Test
    @DisplayName("CA-3-37 con Redis caído guardar una sección sigue funcionando")
    void guardarSigueFuncionando() throws Exception {
        Map<String, Object> identidad = new LinkedHashMap<>();
        identidad.put("nombre", "Sin Redis");
        identidad.put("razonSocial", "Sin Redis S.A.C.");
        identidad.put("ruc", "20123456789");
        identidad.put("direccionFiscal", "Jr. Gamarra 700");
        identidad.put("version", 0);

        mvc.perform(put("/api/v1/admin/settings/identidad").with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(identidad))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identidad.nombre", is("Sin Redis")));
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.identidad.nombre", is("Sin Redis")));
    }
}
