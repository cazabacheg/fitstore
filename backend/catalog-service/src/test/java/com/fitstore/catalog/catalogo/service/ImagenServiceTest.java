package com.fitstore.catalog.catalogo.service;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.pruebas.ApiBase;
import com.fitstore.catalog.pruebas.GaragePrueba;
import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

/** Imágenes de producto con Garage real en Testcontainers (HU-2-06): tipo real, tamaños, color y principal. */
class ImagenServiceTest extends ApiBase {

    @DynamicPropertySource
    static void garage(DynamicPropertyRegistry registro) {
        GaragePrueba.registrar(registro);
    }

    @Autowired FileStorage almacenamiento;

    private static byte[] imagen(String formato, int ancho, int alto, boolean conAlfa) throws IOException {
        BufferedImage img = new BufferedImage(ancho, alto, conAlfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int x = 0; x < ancho; x += 40) {
            g.setColor(new Color((x * 255) / ancho, 120, 200 - (x * 100) / ancho));
            g.fillRect(x, 0, 40, alto);
        }
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }

    private Producto productoConColores(String codigo, String... colores) {
        Categoria c = categoria("Imágenes " + codigo, null);
        Producto p = producto(codigo, "Producto " + codigo, "50.00", EstadoProducto.BORRADOR, c);
        for (String color : colores) {
            variante(p, "M", color, 5, 0);
        }
        sincronizar();
        return p;
    }

    private ResultActions subir(Producto p, MockMultipartFile archivo, Long colorId, Boolean principal) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart("/api/v1/admin/productos/" + p.getId() + "/imagenes").file(archivo);
        if (colorId != null) {
            peticion.param("colorId", colorId.toString());
        }
        if (principal != null) {
            peticion.param("principal", principal.toString());
        }
        return mvc.perform(peticion.with(admin()));
    }

    private static MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, tipo, contenido);
    }

    private BufferedImage descargar(String clave) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(almacenamiento.leer(TipoBucket.PUBLICO, clave)));
    }

    private List<Imagen> guardadas(Producto p) {
        sincronizar();
        return imagenes.findByProductoIdOrderByOrdenAsc(p.getId());
    }

    // ------------------------------------------------------------ CA-2-29

    @Test
    @DisplayName("CA-2-29 un JPG grande se guarda en dos versiones JPG, de 1200 y 400 px de ancho, en el bucket público")
    void subeJpgGrande() throws Exception {
        Producto p = productoConColores("IMG01", "negro");

        subir(p, archivo("foto.jpg", "image/jpeg", imagen("jpg", 3000, 2000, false)), null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes", hasSize(1)))
                .andExpect(jsonPath("$.data.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("negro")))
                .andExpect(jsonPath("$.data.imagenes[0].ancho").value(1200))
                .andExpect(jsonPath("$.data.imagenes[0].alto").value(800))
                .andExpect(jsonPath("$.data.imagenes[0].url").value(org.hamcrest.Matchers.endsWith("-1200.jpg")))
                .andExpect(jsonPath("$.data.imagenes[0].urlMiniatura").value(org.hamcrest.Matchers.endsWith("-400.jpg")));

        Imagen guardada = guardadas(p).get(0);
        assertThat(guardada.getClave()).startsWith("productos/" + p.getId() + "/");
        BufferedImage grande = descargar(guardada.getClave() + "-1200.jpg");
        BufferedImage miniatura = descargar(guardada.getClave() + "-400.jpg");
        assertThat(grande.getWidth()).isEqualTo(1200);
        assertThat(grande.getHeight()).isEqualTo(800);
        assertThat(miniatura.getWidth()).isEqualTo(400);
        assertThat(miniatura.getHeight()).isBetween(266, 267);
        byte[] bytes = almacenamiento.leer(TipoBucket.PUBLICO, guardada.getClave() + "-1200.jpg");
        assertThat(bytes[0] & 0xFF).isEqualTo(0xFF);
        assertThat(bytes[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("CA-2-29 acepta PNG con transparencia y WebP, y no agranda las fotos más chicas que 1200 px")
    void subePngYWebp() throws Exception {
        Producto p = productoConColores("IMG02", "negro");

        subir(p, archivo("logo.png", "image/png", imagen("png", 700, 500, true)), null, null).andExpect(status().isCreated());
        byte[] webp = Files.readAllBytes(Path.of("src/test/resources/imagenes/muestra.webp"));
        subir(p, archivo("foto.webp", "image/webp", webp), null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes", hasSize(2)));

        List<Imagen> lista = guardadas(p);
        assertThat(descargar(lista.get(0).getClave() + "-1200.jpg").getWidth()).isEqualTo(700);
        assertThat(descargar(lista.get(0).getClave() + "-400.jpg").getWidth()).isEqualTo(400);
        assertThat(descargar(lista.get(1).getClave() + "-1200.jpg").getWidth()).isEqualTo(800);
        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, lista.get(1).getClave() + "-400.jpg")).isTrue();
    }

    // ------------------------------------------------------------ CA-2-30

    @Test
    @DisplayName("CA-2-30 un archivo de otro tipo, aunque se llame .jpg, responde 400 con el motivo")
    void rechazaOtroTipo() throws Exception {
        Producto p = productoConColores("IMG03", "negro");

        subir(p, archivo("falsa.jpg", "image/jpeg", "esto no es una imagen".getBytes(StandardCharsets.UTF_8)), null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("JPG, PNG o WebP")));
        subir(p, archivo("animada.jpg", "image/jpeg", imagen("gif", 100, 100, false)), null, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"));
        subir(p, archivo("vacia.jpg", "image/jpeg", new byte[0]), null, null).andExpect(status().isBadRequest());
        // cabecera JPG válida pero contenido roto
        byte[] cortada = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
        subir(p, archivo("rota.jpg", "image/jpeg", cortada), null, null).andExpect(status().isBadRequest());
        assertThat(guardadas(p)).isEmpty();
    }

    @Test
    @DisplayName("CA-2-30 un archivo de más de 5 MB responde 400 con el motivo")
    void rechazaMasDeCincoMb() throws Exception {
        Producto p = productoConColores("IMG04", "negro");
        byte[] enorme = new byte[5 * 1024 * 1024 + 1];
        enorme[0] = (byte) 0xFF;
        enorme[1] = (byte) 0xD8;
        enorme[2] = (byte) 0xFF;

        subir(p, archivo("enorme.jpg", "image/jpeg", enorme), null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_MUY_GRANDE"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("5 MB")));
        assertThat(guardadas(p)).isEmpty();
    }

    // ------------------------------------------------------------ CA-2-31

    @Test
    @DisplayName("CA-2-31 con 10 imágenes no se puede subir otra")
    void maximoDiezImagenes() throws Exception {
        Producto p = productoConColores("IMG05", "negro");
        for (int i = 0; i < 10; i++) {
            imagen(p, "negro", i == 0);
        }
        sincronizar();

        subir(p, archivo("otra.jpg", "image/jpeg", imagen("jpg", 600, 400, false)), null, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("LIMITE_IMAGENES"));
        assertThat(guardadas(p)).hasSize(10);
    }

    // ------------------------------------------------------------ CA-2-32

    @Test
    @DisplayName("CA-2-32 con varios colores el color es obligatorio; con uno solo se asigna solo")
    void colorObligatorio() throws Exception {
        Producto dos = productoConColores("IMG06", "negro", "rojo");
        Producto uno = productoConColores("IMG07", "azul");
        MockMultipartFile foto = archivo("foto.jpg", "image/jpeg", imagen("jpg", 600, 400, false));

        subir(dos, foto, null, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("COLOR_OBLIGATORIO"));
        subir(dos, foto, 999999999L, null).andExpect(status().isNotFound());
        subir(dos, foto, colorId("rojo"), null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("rojo")));
        subir(uno, foto, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("azul")));
    }

    @Test
    @DisplayName("CA-2-32 siempre hay exactamente una principal: la primera lo es, y marcar otra le quita el cargo a la anterior")
    void unaSolaPrincipal() throws Exception {
        Producto p = productoConColores("IMG08", "negro");
        MockMultipartFile foto = archivo("foto.jpg", "image/jpeg", imagen("jpg", 600, 400, false));

        subir(p, foto, null, null).andExpect(status().isCreated());
        subir(p, foto, null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes[?(@.principal==true)]", hasSize(1)));
        subir(p, foto, null, true).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imagenes[?(@.principal==true)]", hasSize(1)))
                .andExpect(jsonPath("$.data.imagenes[2].principal").value(true));

        List<Imagen> lista = guardadas(p);
        assertThat(lista).extracting(Imagen::isPrincipal).containsExactly(false, false, true);
        assertThat(lista).extracting(Imagen::getOrden).containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("CA-2-32 al reordenar, cambiar el color o marcar la principal, se guarda")
    void editarImagen() throws Exception {
        Producto p = productoConColores("IMG09", "negro", "rojo");
        MockMultipartFile foto = archivo("foto.jpg", "image/jpeg", imagen("jpg", 600, 400, false));
        for (int i = 0; i < 3; i++) {
            subir(p, foto, colorId("negro"), null).andExpect(status().isCreated());
        }
        List<Imagen> lista = guardadas(p);
        long primera = lista.get(0).getId();
        long segunda = lista.get(1).getId();
        long tercera = lista.get(2).getId();

        // la tercera pasa al principio, es la principal y cambia a rojo
        mvcPut(tercera, 0, true, colorId("rojo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imagenes[0].id").value(tercera))
                .andExpect(jsonPath("$.data.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("rojo")))
                .andExpect(jsonPath("$.data.imagenes[?(@.principal==true)]", hasSize(1)));

        List<Imagen> reordenada = guardadas(p);
        assertThat(reordenada).extracting(Imagen::getId).containsExactly(tercera, primera, segunda);
        assertThat(reordenada).extracting(Imagen::getOrden).containsExactly(0, 1, 2);
        assertThat(reordenada).extracting(Imagen::isPrincipal).containsExactly(true, false, false);
        mvcPut(primera, 1, false, 999999999L).andExpect(status().isNotFound());
        mvcPut(999999999L, 0, false, colorId("rojo")).andExpect(status().isNotFound());
    }

    private ResultActions mvcPut(long imagenId, int orden, boolean principal, long colorId) throws Exception {
        return mvc.perform(put("/api/v1/admin/imagenes/" + imagenId).with(admin()).contentType(APPLICATION_JSON)
                .content("{\"orden\":" + orden + ",\"principal\":" + principal + ",\"colorId\":" + colorId + "}"));
    }

    @Test
    @DisplayName("CA-2-32 al borrar la principal pasa a serlo la siguiente en orden y se borran sus archivos")
    void borrarLaPrincipal() throws Exception {
        Producto p = productoConColores("IMG10", "negro");
        MockMultipartFile foto = archivo("foto.jpg", "image/jpeg", imagen("jpg", 600, 400, false));
        for (int i = 0; i < 3; i++) {
            subir(p, foto, null, null).andExpect(status().isCreated());
        }
        List<Imagen> lista = guardadas(p);
        Imagen principal = lista.get(0);
        Imagen siguiente = lista.get(1);
        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, principal.getClave() + "-1200.jpg")).isTrue();

        mvc.perform(delete("/api/v1/admin/imagenes/" + principal.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imagenes", hasSize(2)))
                .andExpect(jsonPath("$.data.imagenes[0].id").value(siguiente.getId()))
                .andExpect(jsonPath("$.data.imagenes[0].principal").value(true));

        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, principal.getClave() + "-1200.jpg")).isFalse();
        assertThat(almacenamiento.existe(TipoBucket.PUBLICO, principal.getClave() + "-400.jpg")).isFalse();
        assertThat(guardadas(p)).extracting(Imagen::isPrincipal).containsExactly(true, false);
        mvc.perform(delete("/api/v1/admin/imagenes/" + principal.getId()).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-32 las imágenes son solo para ADMIN y el producto debe existir")
    void permisosYProductoInexistente() throws Exception {
        Producto p = productoConColores("IMG11", "negro");
        MockMultipartFile foto = archivo("foto.jpg", "image/jpeg", imagen("jpg", 600, 400, false));

        mvc.perform(multipart("/api/v1/admin/productos/" + p.getId() + "/imagenes").file(foto).with(operador()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/admin/productos/999999999/imagenes").file(foto).with(admin()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/admin/imagenes/1").with(operador())).andExpect(status().isForbidden());
        JsonNode sinArchivo = cuerpo(mvc.perform(multipart("/api/v1/admin/productos/" + p.getId() + "/imagenes").with(admin()))
                .andExpect(status().isBadRequest()));
        assertThat(sinArchivo.path("codigo").asText()).isEqualTo("VALIDACION");
    }
}
