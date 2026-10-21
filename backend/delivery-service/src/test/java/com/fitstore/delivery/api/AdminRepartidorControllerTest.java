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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Repartidores: los administra el OPERADOR (y el ADMIN) (CA-6-16 a CA-6-18). */
class AdminRepartidorControllerTest extends PruebaApi {

    private int crear(String nombres, String telefono, String vehiculo, String placa) throws Exception {
        String cuerpo = "{\"nombres\":\"" + nombres + "\",\"telefono\":\"" + telefono + "\",\"vehiculo\":\"" + vehiculo
                + "\"" + (placa == null ? "" : ",\"placa\":\"" + placa + "\"") + "}";
        String respuesta = mvc.perform(post("/api/v1/admin/repartidores").with(operador())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.data.id");
    }

    @Test
    @DisplayName("CA-6-16 registra un repartidor con celular de 9 dígitos que empieza con 9 y placa si es moto o auto")
    void registra() throws Exception {
        crear("Juan Pérez", "987654321", "MOTO", "ABC-123");
        crear("Rosa Díaz", "912345678", "BICICLETA", null);

        mvc.perform(get("/api/v1/admin/repartidores").with(operador()))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].tieneEntregas").value(false));

        mvc.perform(post("/api/v1/admin/repartidores").with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Luis Gómez\",\"telefono\":\"887654321\",\"vehiculo\":\"MOTO\",\"placa\":\"ABC-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("telefono"));
        mvc.perform(post("/api/v1/admin/repartidores").with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Luis Gómez\",\"telefono\":\"987654321\",\"vehiculo\":\"AUTO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("placa"));
    }

    @Test
    @DisplayName("CA-6-16 edita un repartidor")
    void edita() throws Exception {
        int id = crear("Juan Pérez", "987654321", "MOTO", "ABC-123");

        mvc.perform(put("/api/v1/admin/repartidores/" + id).with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Juan Pérez Soto\",\"telefono\":\"987654322\",\"vehiculo\":\"AUTO\",\"placa\":\"XYZ987\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombres").value("Juan Pérez Soto"))
                .andExpect(jsonPath("$.data.vehiculo").value("AUTO"));
    }

    @Test
    @DisplayName("CA-6-17 un repartidor desactivado no aparece entre los activos para asignar")
    void desactiva() throws Exception {
        int id = crear("Juan Pérez", "987654321", "MOTO", "ABC-123");
        crear("Rosa Díaz", "912345678", "BICICLETA", null);

        mvc.perform(patch("/api/v1/admin/repartidores/" + id + "/desactivar").with(operador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));

        mvc.perform(get("/api/v1/admin/repartidores").with(operador()).param("activo", "true"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nombres").value("Rosa Díaz"));
        mvc.perform(patch("/api/v1/admin/repartidores/" + id + "/activar").with(operador()))
                .andExpect(jsonPath("$.data.activo").value(true));
    }

    @Test
    @DisplayName("CA-6-18 un repartidor con entregas no se elimina (409); sin entregas, sí")
    void eliminarSegunEntregas() throws Exception {
        int conEntregas = crear("Juan Pérez", "987654321", "MOTO", "ABC-123");
        int libre = crear("Rosa Díaz", "912345678", "BICICLETA", null);
        jdbc.update("""
                INSERT INTO entrega (id, pedido_id, codigo_pedido, modalidad, estado, dest_nombres, dest_apellidos, dest_dni,
                    dest_telefono, dest_email, dir_ubigeo, dir_departamento, dir_provincia, dir_distrito, dir_direccion,
                    repartidor_id, fecha_programada, resumen_items, bultos, creado_en, actualizado_en)
                VALUES (gen_random_uuid(), gen_random_uuid(), 'FS-X', 'DELIVERY_LOCAL', 'ASIGNADA', 'A', 'B', '12345678',
                    '999888777', 'a@b.pe', '150122', 'Lima', 'Lima', 'Miraflores', 'Calle 1', ?, '2026-10-20', '1 x Polo', 1,
                    now(), now())
                """, conEntregas);

        mvc.perform(delete("/api/v1/admin/repartidores/" + conEntregas).with(operador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("desactívalo")));
        mvc.perform(get("/api/v1/admin/repartidores").with(operador()).param("activo", "true"))
                .andExpect(jsonPath("$.data[?(@.nombres=='Juan Pérez')].tieneEntregas").value(true));

        mvc.perform(delete("/api/v1/admin/repartidores/" + libre).with(operador())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/repartidores").with(admin())).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("CA-6-16 sin sesión la API de repartidores responde 401")
    void sinSesion() throws Exception {
        mvc.perform(get("/api/v1/admin/repartidores")).andExpect(status().isUnauthorized());
    }
}
