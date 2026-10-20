package com.fitstore.settings.ubigeo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.settings.pruebas.PruebaApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** HU-3-09: ubigeo del INEI por API pública (CA-3-32 a CA-3-34), con los datos reales de la migración V2. */
class UbigeoApiTest extends PruebaApi {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("CA-3-32 las provincias de un departamento vienen ordenadas por nombre y con caché de 24 h")
    void provinciasOrdenadasConCache() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/departamentos/15/provincias"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.data[0].nombre", is("Barranca")))
                .andExpect(jsonPath("$.data[0].codigo", is("1502")));
    }

    @Test
    @DisplayName("CA-3-32 los departamentos y los distritos de una provincia también llevan caché de 24 h")
    void departamentosYDistritosConCache() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/departamentos"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(jsonPath("$.data", hasSize(25)))
                .andExpect(jsonPath("$.data[0].nombre", is("Amazonas")));
        mvc.perform(get("/api/v1/ubigeo/provincias/1501/distritos"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(jsonPath("$.data", hasSize(44)))
                .andExpect(jsonPath("$.data[0].nombre", is("Ancon")));
    }

    @Test
    @DisplayName("CA-3-32 un departamento o una provincia inexistente responde 404")
    void padreInexistente() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/departamentos/99/provincias")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ubigeo/provincias/9999/distritos")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-3-33 buscar 'miraf' devuelve los Miraflores con su provincia y departamento, sin importar tildes")
    void buscadorSinTildes() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/distritos").param("q", "miraf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.codigo=='150122')].provincia", org.hamcrest.Matchers.contains("Lima")))
                .andExpect(jsonPath("$.data[?(@.codigo=='150122')].departamento", org.hamcrest.Matchers.contains("Lima")))
                .andExpect(jsonPath("$.data[?(@.codigo=='040110')].departamento",
                        org.hamcrest.Matchers.contains("Arequipa")))
                .andExpect(jsonPath("$.data[?(@.nombre=='Miraflores')]", hasSize(4)));
        mvc.perform(get("/api/v1/ubigeo/distritos").param("q", "MIRÁF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.codigo=='150122')]", hasSize(1)));
    }

    @Test
    @DisplayName("CA-3-33 el buscador devuelve como máximo 20 resultados y exige 3 letras")
    void buscadorLimitaYValida() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/distritos").param("q", "san"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(20)));
        mvc.perform(get("/api/v1/ubigeo/distritos").param("q", "mi")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-34 el código 150122 es 'Miraflores, Lima, Lima'")
    void nombreCompletoDeUnDistrito() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/150122"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreCompleto", is("Miraflores, Lima, Lima")))
                .andExpect(jsonPath("$.data.distrito", is("Miraflores")))
                .andExpect(jsonPath("$.data.provincia", is("Lima")))
                .andExpect(jsonPath("$.data.departamento", is("Lima")));
    }

    @Test
    @DisplayName("CA-3-34 acepta también el código de provincia (4) y de departamento (2)")
    void nombreDeProvinciaYDepartamento() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/1501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreCompleto", is("Lima, Lima")));
        mvc.perform(get("/api/v1/ubigeo/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreCompleto", is("Lima")));
    }

    @Test
    @DisplayName("CA-3-34 un código inexistente responde 404 y uno mal formado 400")
    void codigoInexistente() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ubigeo/12345")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-34 los nombres con conectores se escriben como 'San Juan de Miraflores'")
    void nombresConConectores() throws Exception {
        mvc.perform(get("/api/v1/ubigeo/150133"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre", is("San Juan de Miraflores")));
    }
}
