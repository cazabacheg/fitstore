package com.fitstore.delivery.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.delivery.pruebas.PruebaApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Endpoints públicos del checkout contra la semilla de V2 (CA-6-01 a CA-6-06). */
class EnvioControllerTest extends PruebaApi {

    private String cotizar(String modalidad, String ubigeo, Long agenciaId) {
        return "{\"modalidad\":\"" + modalidad + "\",\"ubigeo\":" + (ubigeo == null ? "null" : "\"" + ubigeo + "\"")
                + (agenciaId == null ? "" : ",\"agenciaId\":" + agenciaId) + "}";
    }

    @Test
    @DisplayName("CA-6-01 cotiza una agencia a Arequipa con la tarifa del departamento 04, sin sesión")
    void cotizaConLaTarifaDelDepartamento() throws Exception {
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("AGENCIA", "040101", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.costo").value(18.0))
                .andExpect(jsonPath("$.data.plazoDiasMin").value(2))
                .andExpect(jsonPath("$.data.plazoDiasMax").value(3))
                .andExpect(jsonPath("$.data.nivelTarifa").value("DEPARTAMENTO"))
                .andExpect(jsonPath("$.data.ubigeoTarifa").value("04"));
    }

    @Test
    @DisplayName("CA-6-02 La Victoria usa su tarifa de distrito y Miraflores la de Lima provincia")
    void ganaLaMasEspecifica() throws Exception {
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("DELIVERY_LOCAL", "150115", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.costo").value(7.0))
                .andExpect(jsonPath("$.data.nivelTarifa").value("DISTRITO"));
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("DELIVERY_LOCAL", "150122", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.costo").value(10.0))
                .andExpect(jsonPath("$.data.nivelTarifa").value("PROVINCIA"))
                .andExpect(jsonPath("$.data.ubigeoTarifa").value("1501"));
    }

    @Test
    @DisplayName("CA-6-03 un ubigeo sin 6 dígitos da 400 'Elige el distrito de destino'")
    void exigeElDistrito() throws Exception {
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("AGENCIA", "04", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errores[0].campo").value("ubigeo"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Elige el distrito de destino"));
        mvc.perform(get("/api/v1/envios/opciones").param("ubigeo", "1501"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-6-04 un destino sin tarifa para la modalidad da 422")
    void sinCobertura() throws Exception {
        // La semilla no tiene delivery local en Arequipa (lo rechaza el 400 de CA-6-05) ni agencia en Lima 15
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("AGENCIA", "150122", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SIN_COBERTURA"))
                .andExpect(jsonPath("$.mensaje").value("No realizamos envíos por esta modalidad a este distrito"));
    }

    @Test
    @DisplayName("CA-6-04 una tarifa inactiva no cuenta para cotizar")
    void ignoraLasInactivas() throws Exception {
        jdbc.update("UPDATE tarifa_envio SET activa = false WHERE modalidad = 'AGENCIA' AND ubigeo = '04'");
        try {
            mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                            .content(cotizar("AGENCIA", "040101", null)))
                    .andExpect(status().isUnprocessableContent());
        } finally {
            jdbc.update("UPDATE tarifa_envio SET activa = true WHERE modalidad = 'AGENCIA' AND ubigeo = '04'");
        }
    }

    @Test
    @DisplayName("CA-6-05 el delivery local fuera de Lima y Callao da 400; una agencia inexistente o inactiva, también")
    void validaLocalYAgencia() throws Exception {
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("DELIVERY_LOCAL", "040101", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                        .content(cotizar("AGENCIA", "040101", 99999L)))
                .andExpect(status().isBadRequest());

        jdbc.update("UPDATE agencia SET activa = false WHERE nombre = 'Marvisur'");
        try {
            Long marvisur = jdbc.queryForObject("SELECT id FROM agencia WHERE nombre = 'Marvisur'", Long.class);
            mvc.perform(post("/api/v1/envios/cotizar").contentType(MediaType.APPLICATION_JSON)
                            .content(cotizar("AGENCIA", "040101", marvisur)))
                    .andExpect(status().isBadRequest());
        } finally {
            jdbc.update("UPDATE agencia SET activa = true WHERE nombre = 'Marvisur'");
        }
    }

    @Test
    @DisplayName("CA-6-06 las opciones de Miraflores traen delivery local con costo y plazo, sin sesión")
    void opcionesDeMiraflores() throws Exception {
        mvc.perform(get("/api/v1/envios/opciones").param("ubigeo", "150122"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].modalidad").value("DELIVERY_LOCAL"))
                .andExpect(jsonPath("$.data[0].costo").value(10.0))
                .andExpect(jsonPath("$.data[0].plazoDiasMin").value(1))
                .andExpect(jsonPath("$.data[0].plazoDiasMax").value(2));
        mvc.perform(get("/api/v1/envios/opciones").param("ubigeo", "040101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].modalidad").value("AGENCIA"));
    }

    @Test
    @DisplayName("CA-6-06 GET /agencias devuelve solo las activas, en su orden")
    void agenciasActivasEnOrden() throws Exception {
        jdbc.update("UPDATE agencia SET activa = false WHERE nombre = 'Marvisur'");
        try {
            mvc.perform(get("/api/v1/agencias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].nombre").value("Shalom"))
                    .andExpect(jsonPath("$.data[1].nombre").value("Olva Courier"))
                    .andExpect(jsonPath("$.data[2].nombre").value("Cruz del Sur Cargo"))
                    .andExpect(jsonPath("$.data[0].activa", is(true)));
        } finally {
            jdbc.update("UPDATE agencia SET activa = true WHERE nombre = 'Marvisur'");
        }
    }
}
