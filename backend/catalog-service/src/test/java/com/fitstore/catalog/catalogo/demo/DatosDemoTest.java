package com.fitstore.catalog.catalogo.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.pruebas.GaragePrueba;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Datos de demostración del catálogo con el perfil dev (SPEC-2 sección 6): el SQL repetible de {@code db/dev}, los ids de
 * {@code docs/demo/ids-demo.md}, las fotos que catalog sube a Garage al arrancar y el peso de lo versionado. Usa su propia
 * base de datos: la demo no debe mezclarse con los datos de las demás pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"sin-config-server", "dev"})
class DatosDemoTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/dev");
        GaragePrueba.registrar(registro);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired FileStorage almacenamiento;
    @Autowired CargadorImagenesDemo cargador;

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }

    @Test
    @DisplayName("Demo: 15 productos ACTIVO y 2 en BORRADOR, con el árbol de categorías, guías de tallas y el color Variado")
    void catalogoDeDemostracion() {
        assertThat(contar("SELECT count(*) FROM producto WHERE estado = 'ACTIVO' AND id BETWEEN 1001 AND 1099")).isEqualTo(15);
        assertThat(contar("SELECT count(*) FROM producto WHERE estado = 'BORRADOR' AND id BETWEEN 1001 AND 1099")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM categoria WHERE id BETWEEN 101 AND 199")).isEqualTo(13);
        assertThat(contar("SELECT count(*) FROM categoria WHERE padre_id = 104")).isEqualTo(5);
        assertThat(contar("SELECT count(*) FROM categoria WHERE padre_id = 101")).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM categoria WHERE padre_id IS NULL AND nombre IN ('Casacas y polerones', 'Vestidos', 'Conjuntos', 'Nueva colección')"))
                .isEqualTo(4);
        assertThat(contar("SELECT count(*) FROM color WHERE lower(nombre) = 'variado'")).isOne();
        assertThat(contar("SELECT count(*) FROM guia_tallas WHERE categoria_id IN (101, 104, 111)")).isEqualTo(3);
        // cada producto tiene entre 6 y 15 variantes, con ids en el rango de la demo
        List<Integer> porProducto = jdbc.queryForList("SELECT count(*) FROM variante WHERE producto_id BETWEEN 1001 AND 1099 GROUP BY producto_id", Integer.class);
        assertThat(porProducto).hasSize(17).allSatisfy(n -> assertThat(n).isBetween(6, 15));
        assertThat(contar("SELECT count(*) FROM variante WHERE id NOT BETWEEN 10001 AND 19999")).isZero();
        assertThat(contar("SELECT count(*) FROM imagen WHERE id NOT BETWEEN 20001 AND 29999")).isZero();
    }

    @Test
    @DisplayName("Demo: el stock es variado, con casos agotados y de últimas unidades, y un producto totalmente agotado")
    void stockVariado() {
        assertThat(contar("SELECT count(*) FROM stock WHERE fisico - reservado = 0")).isPositive();
        assertThat(contar("SELECT count(*) FROM stock WHERE fisico - reservado BETWEEN 1 AND 5")).isPositive();
        assertThat(contar("SELECT count(*) FROM stock WHERE fisico - reservado > 5")).isPositive();
        assertThat(contar("SELECT count(*) FROM stock s JOIN variante v ON v.id = s.variante_id WHERE v.producto_id = 1011 AND s.fisico > 0")).isZero();
    }

    @Test
    @DisplayName("Demo: cada secuencia queda después del mayor id de la demo para que lo que cree el admin no choque")
    void secuenciasAjustadas() {
        assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('producto', 'id'))", Long.class)).isGreaterThan(1017L);
        assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('categoria', 'id'))", Long.class)).isGreaterThan(113L);
        assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('variante', 'id'))", Long.class))
                .isGreaterThan(jdbc.queryForObject("SELECT max(id) FROM variante", Long.class));
        assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('imagen', 'id'))", Long.class)).isGreaterThan(20041L);
    }

    @Test
    @DisplayName("Demo: la tienda muestra los 15 productos, sus destacados en orden y el detalle con fotos y variantes")
    void laTiendaLaMuestra() throws Exception {
        mvc.perform(get("/api/v1/productos").param("size", "48"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElementos").value(15));
        mvc.perform(get("/api/v1/productos/destacados"))
                .andExpect(jsonPath("$.data[*].slug", contains("legging-pro-compresion", "top-deportivo-soporte-medio",
                        "jogger-algodon-premium", "polo-dryfit-entrenamiento", "short-running-ligero", "vestido-deportivo-tenis")));
        mvc.perform(get("/api/v1/productos/legging-pro-compresion"))
                .andExpect(jsonPath("$.data.variantes.length()").value(12))
                .andExpect(jsonPath("$.data.imagenes.length()").value(3))
                .andExpect(jsonPath("$.data.tieneGuiaTallas").value(true));
        mvc.perform(get("/api/v1/productos").param("categoria", "polos-y-tops"))
                .andExpect(jsonPath("$.data.totalElementos").value(4));
        mvc.perform(get("/api/v1/productos").param("categoria", "tops").param("disponible", "true"))
                .andExpect(jsonPath("$.data.totalElementos").value(1)); // el otro top está agotado
        mvc.perform(get("/api/v1/productos/jogger-algodon-premium")).andExpect(jsonPath("$.data.tramos.length()").value(2));
        mvc.perform(get("/api/v1/productos/pantalon-cargo-deportivo")).andExpect(jsonPath("$.data.tramos.length()").value(0));
    }

    @Test
    @DisplayName("Demo: al arrancar catalog sube a Garage las dos versiones de cada foto, y volver a correrlo no sube nada")
    void subeLasFotosAGarage() {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT clave FROM imagen WHERE id BETWEEN 20001 AND 29999");

        assertThat(filas).hasSize(42);
        for (Map<String, Object> fila : filas) {
            String clave = (String) fila.get("clave");
            assertThat(almacenamiento.existe(TipoBucket.PUBLICO, clave + "-1200.jpg")).as(clave + "-1200.jpg").isTrue();
            assertThat(almacenamiento.existe(TipoBucket.PUBLICO, clave + "-400.jpg")).as(clave + "-400.jpg").isTrue();
        }
        assertThat(cargador.subirLasQueFaltan()).isZero();
        // si falta una versión, la vuelve a subir
        almacenamiento.eliminar(TipoBucket.PUBLICO, filas.get(0).get("clave") + "-400.jpg");
        assertThat(cargador.subirLasQueFaltan()).isOne();
        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, filas.get(0).get("clave") + "-400.jpg")).isTrue();
    }

    @Test
    @DisplayName("Demo: el SQL repetible se puede aplicar de nuevo sin duplicar ni fallar")
    void sqlIdempotente() {
        int productos = contar("SELECT count(*) FROM producto");
        int variantes = contar("SELECT count(*) FROM variante");
        int imagenes = contar("SELECT count(*) FROM imagen");
        int movimientos = contar("SELECT count(*) FROM movimiento_stock");

        new ResourceDatabasePopulator(new ClassPathResource("db/dev/R__catalogo_demo.sql")).execute(jdbc.getDataSource());

        assertThat(contar("SELECT count(*) FROM producto")).isEqualTo(productos);
        assertThat(contar("SELECT count(*) FROM variante")).isEqualTo(variantes);
        assertThat(contar("SELECT count(*) FROM imagen")).isEqualTo(imagenes);
        assertThat(contar("SELECT count(*) FROM movimiento_stock")).isEqualTo(movimientos);
    }

    @Test
    @DisplayName("Demo: las fotos y sus fuentes pesan en total menos de 10 MB")
    void pesoDeLasFotos() throws IOException {
        long total = peso(Path.of("src/main/resources/demo/imagenes")) + peso(Path.of("../../infra/scripts/semilla-imagenes"));

        assertThat(total).isLessThan(10L * 1024 * 1024);
        assertThat(peso(Path.of("src/main/resources/demo/imagenes"))).isPositive();
    }

    private static long peso(Path carpeta) throws IOException {
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            return archivos.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        }
    }
}
