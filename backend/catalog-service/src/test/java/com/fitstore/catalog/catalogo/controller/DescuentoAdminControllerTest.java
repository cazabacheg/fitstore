package com.fitstore.catalog.catalogo.controller;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Política global de descuentos por volumen (HU-2-08): validaciones e interruptor. */
class DescuentoAdminControllerTest extends ApiBase {

    private ResultActions guardar(String json) throws Exception {
        return mvc.perform(put("/api/v1/admin/descuentos").with(admin()).contentType(APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("CA-2-36 la política global parte con los tramos de la semilla y el interruptor encendido")
    void politicaInicial() throws Exception {
        mvc.perform(get("/api/v1/admin/descuentos").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.tramos", hasSize(3)))
                .andExpect(jsonPath("$.data.tramos[1].unidadesMinimas").value(6))
                .andExpect(jsonPath("$.data.tramos[1].porcentaje").value(10));
    }

    @Test
    @DisplayName("CA-2-36 agrega, edita y quita tramos si unidades y porcentajes son crecientes")
    void guardaTramosValidos() throws Exception {
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":2,\"porcentaje\":3},{\"unidadesMinimas\":5,\"porcentaje\":8.5},"
                + "{\"unidadesMinimas\":10,\"porcentaje\":50}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tramos", hasSize(3)))
                .andExpect(jsonPath("$.data.tramos[1].porcentaje").value(8.5));
        sincronizar();
        mvc.perform(get("/api/v1/admin/descuentos").with(admin()))
                .andExpect(jsonPath("$.data.tramos", hasSize(3)))
                .andExpect(jsonPath("$.data.tramos[0].unidadesMinimas").value(2));
        // quitar tramos: sin tramos no hay descuento
        guardar("{\"activo\":true,\"tramos\":[]}").andExpect(status().isOk()).andExpect(jsonPath("$.data.tramos", empty()));
    }

    @Test
    @DisplayName("CA-2-36 rechaza con 400 y el motivo unidades que no crecen, porcentajes que no crecen o fuera de 0 a 50")
    void rechazaTramosInvalidos() throws Exception {
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":3,\"porcentaje\":5},{\"unidadesMinimas\":3,\"porcentaje\":10}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("unidades")));
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":6,\"porcentaje\":5},{\"unidadesMinimas\":3,\"porcentaje\":10}]}")
                .andExpect(status().isBadRequest());
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":3,\"porcentaje\":10},{\"unidadesMinimas\":6,\"porcentaje\":5}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("porcentajes")));
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":3,\"porcentaje\":51}]}").andExpect(status().isBadRequest());
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":3,\"porcentaje\":-1}]}").andExpect(status().isBadRequest());
        guardar("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":1,\"porcentaje\":5}]}").andExpect(status().isBadRequest());
        // nada de lo rechazado cambió la política
        mvc.perform(get("/api/v1/admin/descuentos").with(admin())).andExpect(jsonPath("$.data.tramos", hasSize(3)));
    }

    @Test
    @DisplayName("CA-2-36 admite hasta 10 tramos y rechaza 11")
    void maximoDiezTramos() throws Exception {
        StringBuilder diez = new StringBuilder("{\"activo\":true,\"tramos\":[");
        for (int i = 1; i <= 11; i++) {
            diez.append(i > 1 ? "," : "").append("{\"unidadesMinimas\":").append(i + 1).append(",\"porcentaje\":").append(i).append("}");
            if (i == 10) {
                guardar(diez + "]}").andExpect(status().isOk()).andExpect(jsonPath("$.data.tramos", hasSize(10)));
            }
        }
        guardar(diez + "]}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-37 al apagar el interruptor global ninguna cotización aplica descuentos y la tienda deja de mostrar etiquetas")
    void interruptorApagado() throws Exception {
        Categoria c = categoria("Interruptor", null);
        Producto p = producto("CA37A", "Con Etiqueta", "50.00", EstadoProducto.ACTIVO, c);
        Variante v = variante(p, "M", "negro", 50, 0);
        sincronizar();
        String cotizacion = "{\"lineas\":[{\"varianteId\":" + v.getId() + ",\"cantidad\":6}]}";
        mvc.perform(post("/api/v1/precios/cotizar").contentType(APPLICATION_JSON).content(cotizacion))
                .andExpect(jsonPath("$.data.descuentoTotal").value(30.0));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido[0].etiquetaDescuento").value("Desde 3 un. −5 %"));

        guardar("{\"activo\":false,\"tramos\":[{\"unidadesMinimas\":3,\"porcentaje\":5},{\"unidadesMinimas\":6,\"porcentaje\":10},"
                + "{\"unidadesMinimas\":12,\"porcentaje\":15}]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));
        sincronizar();

        mvc.perform(post("/api/v1/precios/cotizar").contentType(APPLICATION_JSON).content(cotizacion))
                .andExpect(jsonPath("$.data.descuentoTotal").value(0))
                .andExpect(jsonPath("$.data.total").value(300.0))
                .andExpect(jsonPath("$.data.productos[0].siguienteTramo", nullValue()));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido[0].etiquetaDescuento", nullValue()));
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.tramos", empty()));
    }

    @Test
    @DisplayName("CA-2-36 la política de descuentos es solo para ADMIN")
    void permisos() throws Exception {
        mvc.perform(get("/api/v1/admin/descuentos").with(operador())).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/descuentos").with(operador()).contentType(APPLICATION_JSON)
                        .content("{\"activo\":true,\"tramos\":[]}"))
                .andExpect(status().isForbidden());
    }
}
