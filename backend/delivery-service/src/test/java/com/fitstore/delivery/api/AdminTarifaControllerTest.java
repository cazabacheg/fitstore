package com.fitstore.delivery.api;

import static com.fitstore.delivery.pruebas.Sesiones.admin;
import static com.fitstore.delivery.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.delivery.pruebas.PruebaApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Mantenimiento de tarifas por el ADMIN (CA-6-07 a CA-6-12). */
class AdminTarifaControllerTest extends PruebaApi {

    @AfterEach
    void quitarLasTarifasDePrueba() {
        jdbc.update("DELETE FROM tarifa_envio WHERE costo IN (9.50, 11.00, 12.00) AND ubigeo IN ('150122', '0501', '15')");
    }

    private ResultActions crear(String modalidad, String ubigeo, String costo, int min, int max) throws Exception {
        String cuerpo = "{\"modalidad\":\"" + modalidad + "\",\"ubigeo\":\"" + ubigeo + "\",\"costo\":" + costo
                + ",\"plazoDiasMin\":" + min + ",\"plazoDiasMax\":" + max + "}";
        return mvc.perform(post("/api/v1/admin/tarifas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    @DisplayName("CA-6-07 crea la tarifa con el código del nivel elegido y la lista con su nivel")
    void creaYLista() throws Exception {
        crear("DELIVERY_LOCAL", "150122", "9.50", 1, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ubigeo").value("150122"))
                .andExpect(jsonPath("$.data.nivel").value("DISTRITO"))
                .andExpect(jsonPath("$.data.activa").value(true));
        mvc.perform(get("/api/v1/admin/tarifas").with(admin()).param("modalidad", "DELIVERY_LOCAL").param("nivel", "DISTRITO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.ubigeo=='150122')].costo").value(9.5))
                .andExpect(jsonPath("$.data[?(@.ubigeo=='150101')].nivel").value("DISTRITO"));
        crear("AGENCIA", "0501", "11.00", 2, 4).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nivel").value("PROVINCIA"));
        crear("AGENCIA", "15", "12.00", 2, 4).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nivel").value("DEPARTAMENTO"));
    }

    @Test
    @DisplayName("CA-6-08 repetir modalidad y destino da 409")
    void duplicado() throws Exception {
        crear("DELIVERY_LOCAL", "150122", "9.50", 1, 2).andExpect(status().isCreated());

        crear("DELIVERY_LOCAL", "150122", "9.50", 1, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una tarifa para ese destino"));
    }

    @Test
    @DisplayName("CA-6-09 el local fuera de Lima y Callao, el costo negativo o el plazo invertido dan 400 con el campo")
    void validaciones() throws Exception {
        crear("DELIVERY_LOCAL", "050101", "9.50", 1, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("ubigeo"));
        crear("AGENCIA", "050101", "-1", 1, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("costo"));
        crear("AGENCIA", "050101", "9.50", 5, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("plazoDiasMax"));
    }

    @Test
    @DisplayName("CA-6-10 una tarifa desactivada deja de usarse al cotizar y una eliminada también")
    void desactivarYEliminar() throws Exception {
        String respuesta = crear("AGENCIA", "0501", "11.00", 2, 4).andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(respuesta, "$.data.id");
        String cotizar = "{\"modalidad\":\"AGENCIA\",\"ubigeo\":\"050101\"}";
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON).content(cotizar))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.costo").value(11.0));

        mvc.perform(put("/api/v1/admin/tarifas/" + id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modalidad\":\"AGENCIA\",\"ubigeo\":\"0501\",\"costo\":11.00,\"plazoDiasMin\":2,"
                                + "\"plazoDiasMax\":4,\"activa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(false));
        // Sin la tarifa de la provincia, la cotización cae a la del departamento de la semilla
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON).content(cotizar))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nivelTarifa").value("DEPARTAMENTO"));

        mvc.perform(delete("/api/v1/admin/tarifas/" + id).with(admin())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tarifa_envio WHERE id = ?", Integer.class, id)).isZero();
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON).content(cotizar))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nivelTarifa").value("DEPARTAMENTO"));
    }

    @Test
    @DisplayName("CA-6-12 un OPERADOR recibe 403 en la API de tarifas")
    void operadorNoEntra() throws Exception {
        mvc.perform(get("/api/v1/admin/tarifas").with(operador())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/tarifas").with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modalidad\":\"AGENCIA\",\"ubigeo\":\"15\",\"costo\":12,\"plazoDiasMin\":1,\"plazoDiasMax\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/tarifas")).andExpect(status().isUnauthorized());
    }
}
