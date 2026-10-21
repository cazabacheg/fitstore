package com.fitstore.delivery.api;

import static com.fitstore.delivery.pruebas.Sesiones.admin;
import static com.fitstore.delivery.pruebas.Sesiones.operador;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.delivery.pruebas.PruebaApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Mantenimiento de agencias por el ADMIN (CA-6-13 a CA-6-15). */
class AdminAgenciaControllerTest extends PruebaApi {

    /** Devuelve la semilla de V2 a su estado: las pruebas crean y reordenan agencias. */
    @AfterEach
    void restaurarSemilla() {
        jdbc.update("DELETE FROM agencia WHERE nombre NOT IN ('Shalom', 'Olva Courier', 'Marvisur', 'Cruz del Sur Cargo')");
        jdbc.update("UPDATE agencia SET activa = true, orden = CASE nombre WHEN 'Shalom' THEN 1 WHEN 'Olva Courier' THEN 2 "
                + "WHEN 'Marvisur' THEN 3 ELSE 4 END");
    }

    private int crear(String nombre, String url) throws Exception {
        String cuerpo = "{\"nombre\":\"" + nombre + "\"" + (url == null ? "" : ",\"urlSeguimiento\":\"" + url + "\"") + "}";
        String respuesta = mvc.perform(post("/api/v1/admin/agencias").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.data.id");
    }

    private int idDe(String nombre) {
        return jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = ?", Integer.class, nombre);
    }

    @Test
    @DisplayName("CA-6-13 crea y edita una agencia con una URL https con {codigo}")
    void creaYEdita() throws Exception {
        int id = crear("Transportes Andes", "https://andes.pe/rastreo?codigo={codigo}");

        mvc.perform(put("/api/v1/admin/agencias/" + id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Andes Cargo\",\"urlSeguimiento\":\"https://andes.pe/r/{codigo}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Andes Cargo"))
                .andExpect(jsonPath("$.data.urlSeguimiento").value("https://andes.pe/r/{codigo}"));
    }

    @Test
    @DisplayName("CA-6-13 una URL sin {codigo} o sin https, o un nombre repetido, se rechazan")
    void rechazaInvalidos() throws Exception {
        mvc.perform(post("/api/v1/admin/agencias").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sin Codigo\",\"urlSeguimiento\":\"https://andes.pe/rastreo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("urlSeguimiento"));
        mvc.perform(post("/api/v1/admin/agencias").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sin Https\",\"urlSeguimiento\":\"http://andes.pe/?c={codigo}\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/agencias").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"SHALOM\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CA-6-14 reordenar y desactivar cambian lo que ve el checkout")
    void reordenaYDesactiva() throws Exception {
        mvc.perform(patch("/api/v1/admin/agencias/" + idDe("Cruz del Sur Cargo") + "/orden").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orden\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Cruz del Sur Cargo"));
        mvc.perform(patch("/api/v1/admin/agencias/" + idDe("Olva Courier") + "/desactivar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(false));

        mvc.perform(get("/api/v1/agencias"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].nombre").value("Cruz del Sur Cargo"))
                .andExpect(jsonPath("$.data[1].nombre").value("Shalom"))
                .andExpect(jsonPath("$.data[2].nombre").value("Marvisur"));
        mvc.perform(get("/api/v1/admin/agencias").with(admin())).andExpect(jsonPath("$.data", hasSize(4)));

        mvc.perform(patch("/api/v1/admin/agencias/" + idDe("Olva Courier") + "/activar").with(admin()))
                .andExpect(jsonPath("$.data.activa").value(true));
    }

    @Test
    @DisplayName("CA-6-15 una agencia con entregas no se elimina (409); sin entregas, sí")
    void eliminarSegunEntregas() throws Exception {
        int conEntregas = idDe("Shalom");
        jdbc.update("""
                INSERT INTO entrega (id, pedido_id, codigo_pedido, modalidad, estado, dest_nombres, dest_apellidos, dest_dni,
                    dest_telefono, dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    agencia_id, resumen_items, bultos, creado_en, actualizado_en)
                VALUES (gen_random_uuid(), gen_random_uuid(), 'FS-X', 'AGENCIA', 'PENDIENTE_ENVIO', 'A', 'B', '12345678',
                    '999888777', 'a@b.pe', '040101', 'Arequipa', 'Arequipa', 'Arequipa', 'Calle 1', ?, '1 x Polo', 1, now(), now())
                """, conEntregas);
        mvc.perform(delete("/api/v1/admin/agencias/" + conEntregas).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("desactívala")));

        int libre = crear("Agencia Libre", null);
        mvc.perform(delete("/api/v1/admin/agencias/" + libre).with(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/agencias").with(admin())).andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    @DisplayName("CA-6-13 solo el ADMIN administra agencias")
    void soloAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/agencias").with(operador())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/agencias").with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isForbidden());
    }
}
