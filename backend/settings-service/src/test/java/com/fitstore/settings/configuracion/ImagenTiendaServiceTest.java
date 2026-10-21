package com.fitstore.settings.configuracion;

import static com.fitstore.settings.pruebas.Sesiones.admin;
import static com.fitstore.settings.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.common.storage.TipoBucket;
import com.fitstore.settings.pruebas.AlmacenEnMemoria;
import com.fitstore.settings.pruebas.PruebaApi;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Imágenes de la tienda: tipo real validado, tamaños, reemplazo, landing de hasta 5 (CA-3-07, CA-3-14). */
@Transactional
class ImagenTiendaServiceTest extends PruebaApi {

    private static final String IMAGENES = "/api/v1/admin/settings/imagenes";

    private static byte[] imagen(String formato, int ancho, int alto) throws IOException {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(201, 178, 140));
        g.fillRect(0, 0, ancho, alto);
        g.setColor(new Color(38, 34, 29));
        g.fillOval(ancho / 4, alto / 4, ancho / 2, alto / 2);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }

    private static byte[] recurso(String nombre) throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/imagenes", nombre));
    }

    private static byte[] conRelleno(byte[] cabecera, int total) {
        byte[] bytes = Arrays.copyOf(cabecera, total);
        return bytes;
    }

    private ResultActions subir(String tipo, String nombre, String contentType, byte[] bytes, String alt, String enlace)
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart(IMAGENES)
                .file(new MockMultipartFile("archivo", nombre, contentType, bytes)).param("tipo", tipo);
        if (alt != null) {
            peticion.param("textoAlternativo", alt);
        }
        if (enlace != null) {
            peticion.param("enlace", enlace);
        }
        return mvc.perform(peticion.with(admin()));
    }

    private ResultActions subir(String tipo, String nombre, String contentType, byte[] bytes) throws Exception {
        return subir(tipo, nombre, contentType, bytes, null, null);
    }

    private BufferedImage guardada(String urlPublica) throws IOException {
        String clave = urlPublica.substring(AlmacenEnMemoria.BASE_PUBLICA.length() + 1);
        return ImageIO.read(new ByteArrayInputStream(almacen.leer(TipoBucket.PUBLICO, clave)));
    }

    private String clave(String urlPublica) {
        return urlPublica.substring(AlmacenEnMemoria.BASE_PUBLICA.length() + 1);
    }

    // ---------------------------------------------------------------- logos y favicon (CA-3-07)

    @Test
    @DisplayName("CA-3-07 sube el logo claro en PNG y la tienda lo muestra con su URL pública")
    void subeElLogoClaro() throws Exception {
        subir("LOGO_CLARO", "logo.png", "image/png", imagen("png", 300, 100)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipo", is("LOGO_CLARO")))
                .andExpect(jsonPath("$.data.url", startsWith(AlmacenEnMemoria.BASE_PUBLICA + "/tienda/logo_claro/")))
                .andExpect(jsonPath("$.data.url", endsWith(".png")));

        assertThat(almacen.cantidad()).isEqualTo(1);
        String url = leer(mvc.perform(get("/api/v1/settings/public")).andReturn(), "$.data.identidad.logoClaroUrl");
        assertThat(url).startsWith(AlmacenEnMemoria.BASE_PUBLICA + "/tienda/logo_claro/");
        assertThat(almacen.existe(TipoBucket.PUBLICO, clave(url))).isTrue();
    }

    @Test
    @DisplayName("CA-3-07 el logo oscuro acepta WebP y el favicon acepta PNG o ICO")
    void formatosDeLogoYFavicon() throws Exception {
        subir("LOGO_OSCURO", "logo.webp", "image/webp", recurso("pequena.webp")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url", endsWith(".webp")));
        subir("FAVICON", "icono.ico", "image/x-icon", recurso("icono.ico")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url", endsWith(".ico")));
        subir("FAVICON", "favicon.png", "image/png", imagen("png", 64, 64)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url", endsWith(".png")));
    }

    @Test
    @DisplayName("CA-3-07 una nueva imagen del mismo tipo reemplaza a la anterior y borra su archivo")
    void reemplazaLaAnterior() throws Exception {
        String primera = leer(subir("LOGO_CLARO", "a.png", "image/png", imagen("png", 100, 40)).andReturn(),
                "$.data.url");
        String segunda = leer(subir("LOGO_CLARO", "b.png", "image/png", imagen("png", 200, 80)).andReturn(),
                "$.data.url");

        assertThat(segunda).isNotEqualTo(primera);
        assertThat(almacen.existe(TipoBucket.PUBLICO, clave(primera))).isFalse();
        assertThat(almacen.existe(TipoBucket.PUBLICO, clave(segunda))).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_tienda WHERE tipo = 'LOGO_CLARO'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CA-3-07 un SVG se rechaza aunque venga con extensión y tipo de PNG")
    void rechazaSvg() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        subir("LOGO_CLARO", "logo.svg", "image/svg+xml", svg).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("VALIDACION")));
        subir("LOGO_CLARO", "logo.png", "image/png", svg).andExpect(status().isBadRequest());
        subir("FAVICON", "favicon.ico", "image/x-icon", svg).andExpect(status().isBadRequest());
        assertThat(almacen.cantidad()).isZero();
    }

    @Test
    @DisplayName("CA-3-07 se valida el tipo real: un JPG disfrazado de PNG o texto con extensión de imagen se rechaza")
    void validaElTipoReal() throws Exception {
        subir("LOGO_CLARO", "logo.png", "image/png", imagen("jpg", 100, 40)).andExpect(status().isBadRequest());
        subir("LOGO_CLARO", "logo.png", "image/png", "no soy una imagen".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
        subir("LOGO_CLARO", "logo.ico", "image/x-icon", recurso("icono.ico")).andExpect(status().isBadRequest());
        assertThat(almacen.cantidad()).isZero();
    }

    @Test
    @DisplayName("CA-3-07 los logos y el favicon admiten hasta 2 MB")
    void limiteDeDosMb() throws Exception {
        byte[] cabeceraPng = Arrays.copyOf(imagen("png", 10, 10), 16);
        subir("LOGO_CLARO", "grande.png", "image/png", conRelleno(cabeceraPng, 2 * 1024 * 1024 + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("archivo")));
    }

    @Test
    @DisplayName("CA-3-07 un tipo de imagen desconocido o un archivo vacío responde 400")
    void tipoDesconocidoYVacio() throws Exception {
        subir("BANNER", "a.png", "image/png", imagen("png", 10, 10)).andExpect(status().isBadRequest());
        subir("LOGO_CLARO", "a.png", "image/png", new byte[0]).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-07 solo el ADMIN sube imágenes")
    void soloAdminSube() throws Exception {
        mvc.perform(multipart(IMAGENES).file(new MockMultipartFile("archivo", "a.png", "image/png", imagen("png", 10, 10)))
                .param("tipo", "LOGO_CLARO").with(operador())).andExpect(status().isForbidden());
        mvc.perform(multipart(IMAGENES).file(new MockMultipartFile("archivo", "a.png", "image/png", imagen("png", 10, 10)))
                .param("tipo", "LOGO_CLARO")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-3-07 subir una imagen no sube la versión pero publica ConfiguracionActualizada (IMAGENES)")
    void publicaElEventoSinSubirLaVersion() throws Exception {
        subir("FAVICON", "favicon.png", "image/png", imagen("png", 32, 32)).andExpect(status().isCreated());

        List<String> eventos = eventosDelTipo("ConfiguracionActualizada");
        assertThat(eventos).hasSize(1);
        assertThat((List<String>) com.jayway.jsonpath.JsonPath.read(eventos.get(0), "$.secciones"))
                .containsExactly("IMAGENES");
        assertThat((int) com.jayway.jsonpath.JsonPath.read(eventos.get(0), "$.version")).isZero();
        mvc.perform(get("/api/v1/admin/settings").with(admin())).andExpect(jsonPath("$.data.version", is(0)));
    }

    // ---------------------------------------------------------------- landing (CA-3-14)

    @Test
    @DisplayName("CA-3-14 una imagen de landing de 3000 px se redimensiona a 1600 px y se guarda en JPG")
    void redimensionaLaLanding() throws Exception {
        String url = leer(subir("LANDING", "hero.jpg", "image/jpeg", imagen("jpg", 3000, 1000), "Modelo con leggings",
                "/productos").andExpect(status().isCreated()).andExpect(jsonPath("$.data.orden", is(0)))
                .andExpect(jsonPath("$.data.textoAlternativo", is("Modelo con leggings")))
                .andExpect(jsonPath("$.data.enlace", is("/productos"))).andReturn(), "$.data.url");

        assertThat(url).endsWith(".jpg");
        BufferedImage guardada = guardada(url);
        assertThat(guardada.getWidth()).isEqualTo(1600);
        assertThat(guardada.getHeight()).isBetween(530, 535);
        byte[] bytes = almacen.leer(TipoBucket.PUBLICO, clave(url));
        assertThat(bytes[0] & 0xFF).isEqualTo(0xFF);
        assertThat(bytes[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("CA-3-14 una landing en WebP se guarda como JPG y una más chica que 1600 px no se agranda")
    void landingWebpAJpg() throws Exception {
        String url = leer(subir("LANDING", "hero.webp", "image/webp", recurso("ancha.webp")).andExpect(status().isCreated())
                .andReturn(), "$.data.url");
        assertThat(url).endsWith(".jpg");
        assertThat(guardada(url).getWidth()).isEqualTo(1600);

        String chica = leer(subir("LANDING", "chica.jpg", "image/jpeg", imagen("jpg", 800, 400)).andReturn(), "$.data.url");
        assertThat(guardada(chica).getWidth()).isEqualTo(800);
    }

    @Test
    @DisplayName("CA-3-14 la landing solo acepta JPG o WebP: PNG, SVG y texto se rechazan")
    void landingSoloJpgOWebp() throws Exception {
        subir("LANDING", "hero.png", "image/png", imagen("png", 800, 400)).andExpect(status().isBadRequest());
        subir("LANDING", "hero.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
        assertThat(almacen.cantidad()).isZero();
    }

    @Test
    @DisplayName("CA-3-14 la landing admite hasta 5 MB")
    void limiteDeCincoMb() throws Exception {
        byte[] cabeceraJpg = Arrays.copyOf(imagen("jpg", 10, 10), 16);
        subir("LANDING", "enorme.jpg", "image/jpeg", conRelleno(cabeceraJpg, 5 * 1024 * 1024 + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("archivo")));
    }

    @Test
    @DisplayName("CA-3-14 la sexta imagen de landing se rechaza con 409 LIMITE_IMAGENES")
    void rechazaLaSexta() throws Exception {
        for (int i = 0; i < 5; i++) {
            subir("LANDING", "h" + i + ".jpg", "image/jpeg", imagen("jpg", 800, 400)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.orden", is(i)));
        }
        subir("LANDING", "h6.jpg", "image/jpeg", imagen("jpg", 800, 400)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("LIMITE_IMAGENES")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_tienda WHERE tipo = 'LANDING'", Integer.class))
                .isEqualTo(5);
        assertThat(almacen.cantidad()).isEqualTo(5);
    }

    @Test
    @DisplayName("CA-3-14 se puede reordenar, cambiar el texto alternativo o el enlace, y la landing las muestra en orden")
    void reordenaYEdita() throws Exception {
        long primera = ((Number) leer(subir("LANDING", "a.jpg", "image/jpeg", imagen("jpg", 800, 400)).andReturn(),
                "$.data.id")).longValue();
        long segunda = ((Number) leer(subir("LANDING", "b.jpg", "image/jpeg", imagen("jpg", 800, 400)).andReturn(),
                "$.data.id")).longValue();

        mvc.perform(put(IMAGENES + "/" + primera).with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(java.util.Map.of("orden", 1, "textoAlternativo", "Primera", "enlace", "https://x.pe/a"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orden", is(1)))
                .andExpect(jsonPath("$.data.enlace", is("https://x.pe/a")));
        mvc.perform(put(IMAGENES + "/" + segunda).with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(java.util.Map.of("orden", 0)))).andExpect(status().isOk());

        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(jsonPath("$.data.contenido.imagenesLanding", hasSize(2)))
                .andExpect(jsonPath("$.data.contenido.imagenesLanding[0].id", is((int) segunda)))
                .andExpect(jsonPath("$.data.contenido.imagenesLanding[1].id", is((int) primera)))
                .andExpect(jsonPath("$.data.contenido.imagenesLanding[1].textoAlternativo", is("Primera")));
    }

    @Test
    @DisplayName("CA-3-14 un enlace de imagen que no es ruta de la tienda ni https se rechaza")
    void enlaceDeImagenInvalido() throws Exception {
        long id = ((Number) leer(subir("LANDING", "a.jpg", "image/jpeg", imagen("jpg", 800, 400)).andReturn(),
                "$.data.id")).longValue();
        mvc.perform(put(IMAGENES + "/" + id).with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(java.util.Map.of("orden", 0, "enlace", "javascript:alert(1)"))))
                .andExpect(status().isBadRequest());
        subir("LANDING", "b.jpg", "image/jpeg", imagen("jpg", 800, 400), null, "javascript:alert(1)")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-3-14 al eliminar una imagen se borra de la base y de Garage, y deja de salir en lo público")
    void eliminaLaImagen() throws Exception {
        String url = leer(subir("LANDING", "a.jpg", "image/jpeg", imagen("jpg", 800, 400)).andReturn(), "$.data.url");
        long id = ((Number) leer(mvc.perform(get("/api/v1/settings/public")).andReturn(),
                "$.data.contenido.imagenesLanding[0].id")).longValue();

        mvc.perform(delete(IMAGENES + "/" + id).with(admin())).andExpect(status().isOk());

        assertThat(almacen.existe(TipoBucket.PUBLICO, clave(url))).isFalse();
        mvc.perform(get("/api/v1/settings/public")).andExpect(jsonPath("$.data.contenido.imagenesLanding", hasSize(0)));
        mvc.perform(delete(IMAGENES + "/" + id).with(admin())).andExpect(status().isNotFound());
        mvc.perform(put(IMAGENES + "/" + id).with(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo(java.util.Map.of("orden", 0)))).andExpect(status().isNotFound());
    }
}
