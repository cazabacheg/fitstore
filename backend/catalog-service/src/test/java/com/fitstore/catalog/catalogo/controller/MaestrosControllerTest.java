package com.fitstore.catalog.catalogo.controller;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.pruebas.ApiBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Tallas y colores maestros (HU-2-07). */
class MaestrosControllerTest extends ApiBase {

    private ResultActions crearTalla(String codigo, int orden) throws Exception {
        return mvc.perform(post("/api/v1/admin/tallas").with(admin()).contentType(APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\",\"orden\":" + orden + "}"));
    }

    private ResultActions crearColor(String nombre, String hex) throws Exception {
        return mvc.perform(post("/api/v1/admin/colores").with(admin()).contentType(APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"hex\":\"" + hex + "\"}"));
    }

    @Test
    @DisplayName("CA-2-33 crea una talla y un color activos")
    void creaTallaYColor() throws Exception {
        crearTalla("3XL", 7).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codigo").value("3XL"))
                .andExpect(jsonPath("$.data.orden").value(7))
                .andExpect(jsonPath("$.data.activo").value(true));
        crearColor("Turquesa", "#40E0D0").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Turquesa"))
                .andExpect(jsonPath("$.data.hex").value("#40E0D0"))
                .andExpect(jsonPath("$.data.activo").value(true));
    }

    @Test
    @DisplayName("CA-2-33 no permite tallas ni colores duplicados sin importar mayúsculas")
    void sinDuplicados() throws Exception {
        crearTalla("m", 9).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TALLA_DUPLICADA"));
        crearTalla("xl", 9).andExpect(status().isConflict());
        crearColor("NEGRO", "#000000").andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("COLOR_DUPLICADO"));
        crearColor("Azul Marino", "#000080").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CA-2-33 al editar tampoco se puede repetir el nombre de otra talla o color")
    void sinDuplicadosAlEditar() throws Exception {
        Long m = tallaId("M");
        mvc.perform(put("/api/v1/admin/tallas/" + m).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"codigo\":\"l\",\"orden\":3}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/admin/colores/" + colorId("rojo")).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Verde\",\"hex\":\"#DC2626\"}"))
                .andExpect(status().isConflict());
        // conservar su propio nombre (con otras mayúsculas) sí se puede
        mvc.perform(put("/api/v1/admin/colores/" + colorId("rojo")).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Rojo\",\"hex\":\"#DC2626\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nombre").value("Rojo"));
    }

    @Test
    @DisplayName("CA-2-33 el color exige un hex válido #RRGGBB")
    void hexValido() throws Exception {
        crearColor("Sin Hex", "rojo").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        crearColor("Hex Corto", "#FFF").andExpect(status().isBadRequest());
        crearColor("Hex Raro", "#GG0000").andExpect(status().isBadRequest());
        crearColor("Hex Bueno", "#a1B2c3").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CA-2-34 eliminar una talla o un color usados por alguna variante responde 409; si no se usan, se eliminan")
    void eliminarSegunUso() throws Exception {
        Producto p = producto("CA34A", "Producto con variante", "30.00", EstadoProducto.BORRADOR);
        variante(p, "S", "verde", 1, 0);
        long tallaLibre = cuerpo(crearTalla("4XL", 8)).path("data").path("id").asLong();
        long colorLibre = cuerpo(crearColor("Fucsia", "#FF00FF")).path("data").path("id").asLong();
        sincronizar();

        mvc.perform(delete("/api/v1/admin/tallas/" + tallaId("S")).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TALLA_EN_USO"));
        mvc.perform(delete("/api/v1/admin/colores/" + colorId("verde")).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("COLOR_EN_USO"));
        mvc.perform(delete("/api/v1/admin/tallas/" + tallaLibre).with(admin())).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/colores/" + colorLibre).with(admin())).andExpect(status().isOk());
        sincronizar();
        assertThat(tallas.findById(tallaLibre)).isEmpty();
        assertThat(colores.findById(colorLibre)).isEmpty();
        mvc.perform(delete("/api/v1/admin/tallas/" + tallaLibre).with(admin())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/admin/colores/" + colorLibre).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-35 al reordenar las tallas la tienda las muestra en ese orden")
    void reordenarTallas() throws Exception {
        mvc.perform(put("/api/v1/admin/tallas/" + tallaId("XS")).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"codigo\":\"XS\",\"orden\":10}"))
                .andExpect(status().isOk());
        sincronizar();

        mvc.perform(get("/api/v1/tallas"))
                .andExpect(jsonPath("$.data[*].codigo", contains("S", "M", "L", "XL", "XXL", "XS")));
        mvc.perform(get("/api/v1/admin/tallas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].codigo", contains("S", "M", "L", "XL", "XXL", "XS")));
    }

    @Test
    @DisplayName("CA-2-33 una talla o color desactivados salen del filtro de la tienda pero siguen en la administración")
    void desactivar() throws Exception {
        mvc.perform(put("/api/v1/admin/tallas/" + tallaId("XXL")).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"codigo\":\"XXL\",\"orden\":6,\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));
        mvc.perform(put("/api/v1/admin/colores/" + colorId("beige")).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"beige\",\"hex\":\"#D8C7A8\",\"activo\":false}"))
                .andExpect(status().isOk());
        sincronizar();

        mvc.perform(get("/api/v1/tallas")).andExpect(jsonPath("$.data[*].codigo", not(hasItem("XXL"))));
        mvc.perform(get("/api/v1/colores")).andExpect(jsonPath("$.data[*].nombre", not(hasItem("beige"))));
        mvc.perform(get("/api/v1/admin/tallas").with(admin())).andExpect(jsonPath("$.data[*].codigo", hasItem("XXL")));
        mvc.perform(get("/api/v1/admin/colores").with(admin())).andExpect(jsonPath("$.data[*].nombre", hasItem("beige")));
    }

    @Test
    @DisplayName("CA-2-33 los maestros son solo para ADMIN")
    void permisos() throws Exception {
        mvc.perform(get("/api/v1/admin/tallas").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/colores").with(operador())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/tallas").with(operador()).contentType(APPLICATION_JSON)
                        .content("{\"codigo\":\"ZZ\",\"orden\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/colores")).andExpect(status().isUnauthorized());
    }
}
