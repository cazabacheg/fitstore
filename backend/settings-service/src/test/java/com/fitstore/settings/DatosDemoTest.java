package com.fitstore.settings;

import static com.fitstore.settings.pruebas.Sesiones.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.common.storage.TipoBucket;
import com.fitstore.settings.configuracion.model.TipoImagen;
import com.fitstore.settings.configuracion.repository.ImagenTiendaRepository;
import com.fitstore.settings.pruebas.AlmacenEnMemoria;
import com.fitstore.settings.pruebas.RedisDePrueba;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Datos de demostración (perfiles dev y demo, SPEC-3 sección 6): base propia con las migraciones de {@code db/dev},
 * reloj real, para comprobar los reclamos de fechas relativas y las imágenes de la landing.
 */
@SpringBootTest(properties = {
    "spring.profiles.active=pruebas,dev",
    "spring.flyway.locations=classpath:db/migration,classpath:db/dev"
})
@AutoConfigureMockMvc
class DatosDemoTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        RedisDePrueba.registrar(registro);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ImagenTiendaRepository imagenes;
    @Autowired AlmacenEnMemoria almacen;

    @Test
    @DisplayName("CA-3-01 la demo deja la marca con beneficios, contacto y tienda física en Gamarra")
    void marcaDeLaDemo() throws Exception {
        mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.beneficios", hasSize(3)))
                .andExpect(jsonPath("$.data.contenido.email", is("ventas@fitstore.pe")))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.ubigeoNombre", is("La Victoria, Lima, Lima")))
                .andExpect(jsonPath("$.data.contenido.tiendaFisica.direccion").value(org.hamcrest.Matchers.containsString("Gamarra")));
    }

    @Test
    @DisplayName("CA-3-01 la demo sube las 3 imágenes de landing a Garage y las muestra en orden")
    void imagenesDeLaLanding() throws Exception {
        assertThat(imagenes.countByTipo(TipoImagen.LANDING)).isEqualTo(3);
        var respuesta = mvc.perform(get("/api/v1/settings/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.imagenesLanding", hasSize(3))).andReturn();
        List<String> urls = JsonPath.read(respuesta.getResponse().getContentAsString(),
                "$.data.contenido.imagenesLanding[*].url");
        for (String url : urls) {
            String clave = url.substring(AlmacenEnMemoria.BASE_PUBLICA.length() + 1);
            assertThat(almacen.existe(TipoBucket.PUBLICO, clave)).as(clave).isTrue();
            assertThat(almacen.leer(TipoBucket.PUBLICO, clave).length).isGreaterThan(50_000);
        }
        assertThat(jdbc.queryForList("SELECT texto_alternativo FROM imagen_tienda WHERE tipo = 'LANDING'", String.class))
                .allMatch(t -> t != null && !t.isBlank());
    }

    @Test
    @DisplayName("CA-3-28 la demo trae 3 reclamos: uno vencido, uno por vencer y uno respondido")
    void reclamosDeLaDemo() throws Exception {
        var resultado = mvc.perform(get("/api/v1/admin/reclamos").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos", is(3))).andReturn();
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(cuerpo, "$.data.contenido[?(@.vencido==true)]")).hasSize(1);
        assertThat((List<?>) JsonPath.read(cuerpo, "$.data.contenido[?(@.porVencer==true)]")).hasSize(1);
        assertThat((List<?>) JsonPath.read(cuerpo, "$.data.contenido[?(@.estado=='RESPONDIDO')]")).hasSize(1);
        assertThat((List<String>) JsonPath.read(cuerpo, "$.data.contenido[*].numero"))
                .allMatch(n -> n.matches("RC-\\d{4}-00000[123]"));
    }

    @Test
    @DisplayName("CA-3-26 el siguiente reclamo real continúa después de los de la demostración")
    void elCorrelativoContinua() {
        assertThat(jdbc.queryForObject("SELECT max(ultimo) FROM correlativo_reclamo", Integer.class)).isGreaterThanOrEqualTo(3);
    }
}
