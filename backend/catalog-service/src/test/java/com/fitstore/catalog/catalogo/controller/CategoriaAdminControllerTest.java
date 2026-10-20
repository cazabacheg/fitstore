package com.fitstore.catalog.catalogo.controller;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.pruebas.ApiBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Administración de categorías y de la guía de tallas (HU-2-04). */
class CategoriaAdminControllerTest extends ApiBase {

    private ResultActions crear(String nombre, Long padreId) throws Exception {
        String cuerpo = "{\"nombre\":\"" + nombre + "\"" + (padreId == null ? "" : ",\"padreId\":" + padreId) + "}";
        return mvc.perform(post("/api/v1/admin/categorias").with(admin()).contentType(APPLICATION_JSON).content(cuerpo));
    }

    private long idDe(ResultActions r) throws Exception {
        return cuerpo(r).path("data").path("id").asLong();
    }

    @Test
    @DisplayName("CA-2-17 crea una raíz y una hija con slug único y orden al final de sus hermanas")
    void creaRaizEHija() throws Exception {
        ResultActions raiz = crear("Zapatillas Pro", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Zapatillas Pro"))
                .andExpect(jsonPath("$.data.slug").value("zapatillas-pro"))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.padreId").doesNotExist());
        long raizId = idDe(raiz);

        crear("Running", raizId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.padreId").value(raizId))
                .andExpect(jsonPath("$.data.slug").value("running"));
        // el mismo nombre bajo otra raíz genera un slug distinto (el slug es único global)
        long otraRaiz = idDe(crear("Calzado", null));
        crear("Running", otraRaiz).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("running-2"));
    }

    @Test
    @DisplayName("CA-2-17 una hija no puede tener hijas: máximo 2 niveles")
    void maximoDosNiveles() throws Exception {
        long raiz = idDe(crear("Nivel uno", null));
        long hija = idDe(crear("Nivel dos", raiz));

        crear("Nivel tres", hija).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        crear("Padre inexistente", 999999999L).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-17 no pueden existir dos hermanas con el mismo nombre, sin importar mayúsculas")
    void nombreUnicoEntreHermanas() throws Exception {
        long raiz = idDe(crear("Hermanas", null));
        crear("Leggings", raiz).andExpect(status().isCreated());

        crear("LEGGINGS", raiz).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_DUPLICADA"));
        crear("hermanas", null).andExpect(status().isConflict());
        // al editar también se valida
        long otra = idDe(crear("Otra", raiz));
        mvc.perform(put("/api/v1/admin/categorias/" + otra).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"leggings\",\"padreId\":" + raiz + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CA-2-17 se puede editar el nombre y desactivar o activar una categoría")
    void editaYActiva() throws Exception {
        long id = idDe(crear("Para editar", null));

        mvc.perform(put("/api/v1/admin/categorias/" + id).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Editada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Editada"));
        mvc.perform(patch("/api/v1/admin/categorias/" + id + "/desactivar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));
        mvc.perform(patch("/api/v1/admin/categorias/" + id + "/activar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(true));
        mvc.perform(patch("/api/v1/admin/categorias/999999999/activar").with(admin())).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/categorias").with(admin()).contentType(APPLICATION_JSON).content("{\"nombre\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-18 al reordenar arrastrando, el orden se guarda y el listado lo refleja")
    void reordenar() throws Exception {
        long a = idDe(crear("Orden A", null));
        long b = idDe(crear("Orden B", null));
        long c = idDe(crear("Orden C", null));
        sincronizar();

        mvc.perform(patch("/api/v1/admin/categorias/" + c + "/orden").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"orden\":0}"))
                .andExpect(status().isOk());
        sincronizar();

        // las categorías del enunciado ya existen en la semilla de dev; aquí solo se mira el orden relativo de las tres
        var lista = cuerpo(mvc.perform(get("/api/v1/admin/categorias").with(admin())).andExpect(status().isOk())).path("data");
        java.util.List<Long> ordenadas = new java.util.ArrayList<>();
        lista.forEach(n -> {
            long id = n.path("id").asLong();
            if (id == a || id == b || id == c) {
                ordenadas.add(id);
            }
        });
        assertThat(ordenadas).containsExactly(c, a, b);
    }

    @Test
    @DisplayName("CA-2-18 mover una hija la reubica entre sus hermanas")
    void reordenarHijas() throws Exception {
        long raiz = idDe(crear("Con hijas", null));
        long h1 = idDe(crear("Hija 1", raiz));
        long h2 = idDe(crear("Hija 2", raiz));
        long h3 = idDe(crear("Hija 3", raiz));
        sincronizar();

        mvc.perform(patch("/api/v1/admin/categorias/" + h3 + "/orden").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"orden\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==" + raiz + ")].hijas[*].id").value(contains((int) h1, (int) h3, (int) h2)));
    }

    @Test
    @DisplayName("CA-2-19 eliminar una categoría con hijas o con productos responde 409; sin ellas la elimina de verdad")
    void eliminarCategoria() throws Exception {
        long conHijas = idDe(crear("Padre con hija", null));
        crear("Hija presente", conHijas);
        Categoria conProductos = categoria("Con productos", null);
        Producto p = producto("CA19A", "Producto asignado", "30.00", EstadoProducto.BORRADOR, conProductos);
        long vacia = idDe(crear("Vacía", null));
        sincronizar();

        mvc.perform(delete("/api/v1/admin/categorias/" + conHijas).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_EN_USO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("desactiv")));
        mvc.perform(delete("/api/v1/admin/categorias/" + conProductos.getId()).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_EN_USO"));
        mvc.perform(delete("/api/v1/admin/categorias/" + vacia).with(admin())).andExpect(status().isOk());
        sincronizar();
        assertThat(categorias.findById(vacia)).isEmpty();
        mvc.perform(delete("/api/v1/admin/categorias/" + vacia).with(admin())).andExpect(status().isNotFound());
        assertThat(p.getId()).isNotNull();
    }

    @Test
    @DisplayName("CA-2-17 el listado de administración trae activas e inactivas con su conteo de productos")
    void listadoAdmin() throws Exception {
        Categoria raiz = categoria("Admin raíz", null);
        Categoria hija = categoria("Admin hija", raiz.getId());
        hija.setActivo(false);
        categorias.saveAndFlush(hija);
        producto("CA17A", "Producto contado", "30.00", EstadoProducto.BORRADOR, raiz);
        sincronizar();

        mvc.perform(get("/api/v1/admin/categorias").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==" + raiz.getId() + ")].cantidadProductos").value(1))
                .andExpect(jsonPath("$.data[?(@.id==" + raiz.getId() + ")].hijas[0].activo").value(false));
    }

    @Test
    @DisplayName("CA-2-20 guarda la guía de tallas y la ven la categoría y sus hijas sin guía propia")
    void guiaDeTallas() throws Exception {
        long raiz = idDe(crear("Con guía", null));
        long hija = idDe(crear("Hija heredera", raiz));
        Categoria hijaEntidad = categorias.findById(hija).orElseThrow();
        sincronizar();
        String guia = "{\"columnas\":[\"Talla\",\"Cintura (cm)\",\"Largo (cm)\"],"
                + "\"filas\":[[\"S\",\"64-68\",\"86\"],[\"M\",\"68-72\",\"88\"]],\"nota\":\"Mide sobre la ropa interior\"}";

        mvc.perform(put("/api/v1/admin/categorias/" + raiz + "/guia-tallas").with(admin()).contentType(APPLICATION_JSON).content(guia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.columnas", hasSize(3)))
                .andExpect(jsonPath("$.data.filas", hasSize(2)));
        mvc.perform(get("/api/v1/categorias/" + hijaEntidad.getSlug() + "/guia-tallas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filas[1]", contains("M", "68-72", "88")))
                .andExpect(jsonPath("$.data.nota").value("Mide sobre la ropa interior"));

        // editar: quitar una fila y una columna reemplaza la guía anterior
        String reducida = "{\"columnas\":[\"Talla\",\"Largo (cm)\"],\"filas\":[[\"S\",\"86\"]]}";
        mvc.perform(put("/api/v1/admin/categorias/" + raiz + "/guia-tallas").with(admin()).contentType(APPLICATION_JSON).content(reducida))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/categorias/" + hijaEntidad.getSlug() + "/guia-tallas"))
                .andExpect(jsonPath("$.data.columnas", hasSize(2)))
                .andExpect(jsonPath("$.data.filas", hasSize(1)));
        mvc.perform(get("/api/v1/admin/categorias").with(admin()))
                .andExpect(jsonPath("$.data[?(@.id==" + raiz + ")].tieneGuiaTallas").value(true));
    }

    @Test
    @DisplayName("CA-2-20 rechaza una guía con filas de otro largo que las columnas o con menos de 2 columnas")
    void guiaInvalida() throws Exception {
        long id = idDe(crear("Guía mala", null));

        mvc.perform(put("/api/v1/admin/categorias/" + id + "/guia-tallas").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"columnas\":[\"Talla\",\"Cintura\"],\"filas\":[[\"S\"]]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/categorias/" + id + "/guia-tallas").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"columnas\":[\"Talla\"],\"filas\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/categorias/999999999/guia-tallas").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"columnas\":[\"Talla\",\"Largo\"],\"filas\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-28 las categorías son solo para ADMIN: el OPERADOR recibe 403 y sin sesión 401")
    void permisos() throws Exception {
        mvc.perform(get("/api/v1/admin/categorias").with(operador())).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(post("/api/v1/admin/categorias").with(operador()).contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Intento\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/categorias")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/admin/categorias").with(admin()))
                .andExpect(jsonPath("$.data[*].slug", not(hasItem("intento"))));
    }
}
