package com.fitstore.catalog.catalogo.service;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * Cachés en Redis real (contenedor {@code redis:8.10.1}): invalidación al editar y degradación sin Redis
 * (HU-2-11: CA-2-56 y CA-2-57; y el menú de CA-2-18).
 */
class CacheCatalogoTest extends ApiBase {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10.1").withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", REDIS::getHost);
        registro.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registro.add("fitstore.catalogo.cache.habilitado", () -> "true");
    }

    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void vaciar() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @AfterEach
    void reanudar() {
        if (Boolean.TRUE.equals(REDIS.getDockerClient().inspectContainerCmd(REDIS.getContainerId()).exec().getState().getPaused())) {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    private Producto activoConStock(String codigo, String nombre, Categoria categoria, int fisico) {
        Producto p = producto(codigo, nombre, "50.00", EstadoProducto.ACTIVO, categoria);
        variante(p, "M", "negro", fisico, 0);
        imagen(p, "negro", true);
        sincronizar();
        return p;
    }

    private String detalle(Producto p) throws Exception {
        return mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    // ------------------------------------------------------------ CA-2-56

    @Test
    @DisplayName("CA-2-56 el detalle del producto se guarda en Redis con el prefijo catalog: y al leerlo de ahí queda igual")
    void guardaElDetalle() throws Exception {
        Producto p = activoConStock("CAC01", "Producto en caché", categoria("Caché uno", null), 20);

        String primera = detalle(p);

        assertThat(redis.hasKey("catalog:producto:" + p.getSlug())).isTrue();
        assertThat(redis.getExpire("catalog:producto:" + p.getSlug())).isBetween(1L, 600L);
        assertThat(detalle(p)).isEqualTo(primera);
    }

    @Test
    @DisplayName("CA-2-56 el stock nunca sale de la caché: el detalle cacheado refleja al instante los cambios de disponibilidad")
    void elStockNoSeCachea() throws Exception {
        Producto p = activoConStock("CAC02", "Stock vivo", categoria("Caché dos", null), 20);
        Variante v = variantes.findByProductoIdOrderByIdAsc(p.getId()).get(0);
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.variantes[0].disponibilidad").value("DISPONIBLE"));
        String guardado = redis.opsForValue().get("catalog:producto:" + p.getSlug());
        assertThat(guardado).doesNotContain("disponibilidad").doesNotContain("fisico").doesNotContain("conStock");

        mvc.perform(post("/api/v1/admin/inventario/ajustes").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"varianteId\":" + v.getId() + ",\"cantidad\":-17,\"motivo\":\"Merma\"}"))
                .andExpect(status().isCreated());
        sincronizar();

        assertThat(redis.hasKey("catalog:producto:" + p.getSlug())).isTrue(); // el ajuste no invalida el detalle...
        mvc.perform(get("/api/v1/productos/" + p.getSlug())) // ...pero la disponibilidad se lee de la base
                .andExpect(jsonPath("$.data.variantes[0].disponibilidad").value("ULTIMAS"))
                .andExpect(jsonPath("$.data.variantes[0].ultimasUnidades").value(3));
        mvc.perform(post("/api/v1/admin/inventario/ajustes").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"varianteId\":" + v.getId() + ",\"cantidad\":-3,\"motivo\":\"Agotado\"}"))
                .andExpect(status().isCreated());
        sincronizar();
        mvc.perform(get("/api/v1/productos/" + p.getSlug()))
                .andExpect(jsonPath("$.data.variantes[0].disponibilidad").value("AGOTADO"))
                .andExpect(jsonPath("$.data.colores[0].conStock").value(false));
    }

    @Test
    @DisplayName("CA-2-56 al editar el producto, sus variantes o su descuento la tienda ve el cambio en la siguiente carga")
    void invalidaAlEditar() throws Exception {
        Producto p = activoConStock("CAC03", "Nombre original", categoria("Caché tres", null), 20);
        Variante v = variantes.findByProductoIdOrderByIdAsc(p.getId()).get(0);
        detalle(p);
        assertThat(redis.hasKey("catalog:producto:" + p.getSlug())).isTrue();

        mvc.perform(put("/api/v1/admin/productos/" + p.getId()).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"CAC03\",\"nombre\":\"Nombre nuevo\",\"descripcion\":\"x\",\"precioBase\":50,\"version\":" + p.getVersion() + "}"))
                .andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:producto:" + p.getSlug())).isFalse();
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.nombre").value("Nombre nuevo"));

        mvc.perform(put("/api/v1/admin/variantes/" + v.getId()).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"" + v.getSku() + "\",\"precio\":39.9}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.variantes[0].precio").value(39.9))
                .andExpect(jsonPath("$.data.precioDesde").value(39.9));

        mvc.perform(put("/api/v1/admin/productos/" + p.getId() + "/descuento").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"modo\":\"SIN_DESCUENTO\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.tramos").isEmpty());
    }

    @Test
    @DisplayName("CA-2-56 al cambiar la política global de descuentos, las imágenes o desactivar el producto, el detalle se renueva")
    void invalidaPoliticaImagenesYEstado() throws Exception {
        Producto p = activoConStock("CAC04", "Cambios varios", categoria("Caché cuatro", null), 20);
        detalle(p);
        mvc.perform(get("/api/v1/admin/descuentos").with(admin())).andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:descuentos:global")).isTrue();

        mvc.perform(put("/api/v1/admin/descuentos").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"activo\":true,\"tramos\":[{\"unidadesMinimas\":2,\"porcentaje\":7}]}"))
                .andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:descuentos:global")).isFalse();
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.tramos[0].porcentaje").value(7));

        Long imagenId = imagenes.findByProductoIdOrderByOrdenAsc(p.getId()).get(0).getId();
        mvc.perform(put("/api/v1/admin/imagenes/" + imagenId).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"orden\":0,\"principal\":true,\"colorId\":" + colorId("rojo") + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("rojo")));

        mvc.perform(patch("/api/v1/admin/productos/" + p.getId() + "/desactivar").with(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-56 los destacados y el menú se guardan en Redis y se renuevan al marcar un destacado o reordenar categorías")
    void destacadosYMenu() throws Exception {
        Categoria c = categoria("Caché cinco", null);
        Categoria otra = categoria("Caché cinco bis", null);
        Producto a = activoConStock("CAC05", "Destacable A", c, 20);
        Producto b = activoConStock("CAC06", "Destacable B", c, 20);

        mvc.perform(get("/api/v1/productos/destacados")).andExpect(jsonPath("$.data").isEmpty());
        assertThat(redis.hasKey("catalog:destacados")).isTrue();
        mvc.perform(patch("/api/v1/admin/productos/" + a.getId() + "/destacado").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"destacado\":true,\"orden\":1}"))
                .andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:destacados")).isFalse();
        mvc.perform(get("/api/v1/productos/destacados")).andExpect(jsonPath("$.data[*].nombre", contains("Destacable A")));

        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:categorias:arbol")).isTrue();
        mvc.perform(patch("/api/v1/admin/categorias/" + otra.getId() + "/orden").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"orden\":0}"))
                .andExpect(status().isOk());
        assertThat(redis.hasKey("catalog:categorias:arbol")).isFalse();
        var menu = cuerpo(mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk())).path("data");
        assertThat(menu.get(0).path("id").asLong()).isEqualTo(otra.getId());
        assertThat(b.getId()).isNotNull();
    }

    // ------------------------------------------------------------ CA-2-57

    @Test
    @DisplayName("CA-2-57 con Redis caído el catálogo sigue funcionando leyendo de la base de datos, y se recupera al volver")
    void redisCaido() throws Exception {
        Categoria c = categoria("Caché seis", null);
        Producto p = activoConStock("CAC07", "Sobrevive sin Redis", c, 20);
        String conRedis = detalle(p);

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();

        assertThat(detalle(p)).isEqualTo(conRedis);
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contenido[0].nombre").value("Sobrevive sin Redis"));
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/destacados")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/descuentos").with(admin())).andExpect(status().isOk());
        // el admin también puede seguir editando: la invalidación que no se puede hacer no rompe la operación
        mvc.perform(put("/api/v1/admin/productos/" + p.getId()).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"CAC07\",\"nombre\":\"Editado sin Redis\",\"descripcion\":\"x\",\"precioBase\":50,\"version\":" + p.getVersion() + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.nombre").value("Editado sin Redis"));

        REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();

        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.nombre").value("Editado sin Redis"));
        assertThat(redis.hasKey("catalog:producto:" + p.getSlug())).isTrue();
    }
}
