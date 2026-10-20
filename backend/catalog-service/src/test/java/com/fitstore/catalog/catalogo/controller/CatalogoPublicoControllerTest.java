package com.fitstore.catalog.catalogo.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.GuiaTallas;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.GuiaTallasRepository;
import com.fitstore.catalog.pruebas.ApiBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Catálogo público: listado, filtros, detalle y guía de tallas (HU-2-01 y la parte de API de HU-2-02). */
class CatalogoPublicoControllerTest extends ApiBase {

    @Autowired GuiaTallasRepository guias;

    private Producto activo(String codigo, String nombre, String precio, Categoria... cats) {
        return producto(codigo, nombre, precio, EstadoProducto.ACTIVO, cats);
    }

    @Test
    @DisplayName("CA-2-01 una categoría padre lista los productos ACTIVO de ella y de sus hijas, cada uno una sola vez")
    void categoriaPadreIncluyeHijasSinRepetir() throws Exception {
        Categoria padre = categoria("Leggings y pantalones", null);
        Categoria hija = categoria("Leggings", padre.getId());
        Categoria otra = categoria("Vestidos", null);
        Producto enAmbas = activo("CA01A", "Legging Doble", "59.90", padre, hija);
        Producto soloHija = activo("CA01B", "Legging Hija", "49.90", hija);
        Producto ajeno = activo("CA01C", "Vestido Ajeno", "89.90", otra);
        Producto borrador = producto("CA01D", "Legging Borrador", "49.90", EstadoProducto.BORRADOR, hija);
        for (Producto p : List.of(enAmbas, soloHija, ajeno, borrador)) {
            variante(p, "M", "negro", 5, 0);
        }
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", padre.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElementos").value(2))
                .andExpect(jsonPath("$.data.contenido[*].nombre", containsInAnyOrder("Legging Doble", "Legging Hija")));
    }

    @Test
    @DisplayName("CA-2-02 la búsqueda encuentra el producto sin importar tildes ni mayúsculas")
    void busquedaSinTildesNiMayusculas() throws Exception {
        Categoria c = categoria("Polos", null);
        Producto polo = activo("CA02A", "Polo Algodón", "29.90", c);
        Producto otro = activo("CA02B", "Short Licra", "39.90", c);
        variante(polo, "M", "negro", 5, 0);
        variante(otro, "M", "negro", 5, 0);
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("q", "polo algodon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Polo Algodón")));
        mvc.perform(get("/api/v1/productos").param("q", "POLO ALGODÓN"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Polo Algodón")));
    }

    @Test
    @DisplayName("CA-2-03 filtra por talla, color, rango de precio y solo disponibles con la misma variante")
    void filtrosCombinados() throws Exception {
        Categoria c = categoria("Leggings", null);
        Producto cumple = activo("CA03A", "Cumple Todo", "50.00", c);
        variante(cumple, "M", "negro", 3, 0);
        Producto sinStock = activo("CA03B", "Sin Stock", "50.00", c);
        variante(sinStock, "M", "negro", 3, 3);
        Producto otroColor = activo("CA03C", "Otro Color", "50.00", c);
        variante(otroColor, "M", "blanco", 5, 0);
        Producto caro = activo("CA03D", "Muy Caro", "80.00", c);
        variante(caro, "M", "negro", 5, 0);
        Producto repartido = activo("CA03E", "Repartido", "50.00", c);
        variante(repartido, "S", "negro", 5, 0);
        variante(repartido, "M", "blanco", 5, 0);
        sincronizar();

        mvc.perform(get("/api/v1/productos")
                        .param("talla", "M").param("color", colorId("negro").toString())
                        .param("precioMin", "40").param("precioMax", "60").param("disponible", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Cumple Todo")));
        // sin "solo disponibles" también aparece el que no tiene stock
        mvc.perform(get("/api/v1/productos")
                        .param("talla", "M").param("color", colorId("negro").toString())
                        .param("precioMin", "40").param("precioMax", "60"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", containsInAnyOrder("Cumple Todo", "Sin Stock")));
    }

    @Test
    @DisplayName("CA-2-04 ordena por precio, novedad y nombre sobre lo filtrado, con 24 por página")
    void ordenYPaginacion() throws Exception {
        Categoria c = categoria("Orden", null);
        Producto b = activo("CA04B", "Beta", "30.00", c);
        Producto a = activo("CA04A", "Alfa", "50.00", c);
        Producto g = activo("CA04G", "Gamma", "40.00", c);
        for (Producto p : List.of(b, a, g)) {
            variante(p, "M", "negro", 5, 0);
        }
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "precio,asc"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Beta", "Gamma", "Alfa")));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "precio,desc"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Alfa", "Gamma", "Beta")));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "nombre"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Alfa", "Beta", "Gamma")));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "novedad"))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Gamma", "Alfa", "Beta")));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.tamano").value(24));
    }

    @Test
    @DisplayName("CA-2-04 la página trae 24 productos por defecto y no admite más de 48")
    void tamanoDePagina() throws Exception {
        Categoria c = categoria("Muchos", null);
        for (int i = 1; i <= 25; i++) {
            Producto p = activo(String.format("CA04M%02d", i), "Producto " + i, "20.00", c);
            variante(p, "M", "negro", 5, 0);
        }
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(24)))
                .andExpect(jsonPath("$.data.totalElementos").value(25))
                .andExpect(jsonPath("$.data.totalPaginas").value(2));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("page", "1"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)));
        mvc.perform(get("/api/v1/productos").param("size", "49"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    @DisplayName("CA-2-05 la tarjeta trae miniatura, desde, colores con stock, etiqueta de descuento y agotado")
    void contenidoDeLaTarjeta() throws Exception {
        Categoria c = categoria("Tarjetas", null);
        Producto conStock = activo("CA05A", "Con Stock", "59.90", c);
        variante(conStock, "M", "negro", 5, 0);
        variante(conStock, "M", "rojo", 0, 0);
        Imagen principal = imagen(conStock, "negro", true);
        Producto agotado = activo("CA05B", "Agotado", "39.90", c);
        variante(agotado, "M", "negro", 2, 2);
        sincronizar();

        var respuesta = mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "nombre"))
                .andExpect(status().isOk());
        respuesta
                .andExpect(jsonPath("$.data.contenido[0].nombre").value("Agotado"))
                .andExpect(jsonPath("$.data.contenido[0].agotado").value(true))
                .andExpect(jsonPath("$.data.contenido[0].colores", empty()))
                .andExpect(jsonPath("$.data.contenido[0].imagenUrl", nullValue()))
                .andExpect(jsonPath("$.data.contenido[1].nombre").value("Con Stock"))
                .andExpect(jsonPath("$.data.contenido[1].agotado").value(false))
                .andExpect(jsonPath("$.data.contenido[1].precioDesde").value(59.9))
                .andExpect(jsonPath("$.data.contenido[1].colores", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[1].colores[0].nombre").value("negro"))
                .andExpect(jsonPath("$.data.contenido[1].colores[0].hex").value("#1D1D1F"))
                .andExpect(jsonPath("$.data.contenido[1].etiquetaDescuento").value("Desde 3 un. −5 %"))
                .andExpect(jsonPath("$.data.contenido[1].imagenUrl").value("http://media.prueba/media/" + principal.getClave() + "-400.jpg"));
    }

    @Test
    @DisplayName("CA-2-05 sin descuentos activos la tarjeta no trae etiqueta")
    void tarjetaSinEtiquetaSiNoHayDescuento() throws Exception {
        Categoria c = categoria("Sin etiqueta", null);
        Producto p = activo("CA05C", "Sin Descuento", "59.90", c);
        p.setModoDescuento(ModoDescuento.SIN_DESCUENTO);
        productos.saveAndFlush(p);
        variante(p, "M", "negro", 5, 0);
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido[0].etiquetaDescuento", nullValue()));
    }

    @Test
    @DisplayName("CA-2-06 con filtros sin resultados la lista viene vacía")
    void sinResultados() throws Exception {
        mvc.perform(get("/api/v1/productos").param("q", "producto-que-no-existe-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", empty()))
                .andExpect(jsonPath("$.data.totalElementos").value(0));
    }

    @Test
    @DisplayName("CA-2-07 un producto en BORRADOR o INACTIVO no aparece en listados ni búsquedas y su URL responde 404")
    void productosNoActivos() throws Exception {
        Categoria c = categoria("Estados", null);
        Producto borrador = producto("CA07A", "Borrador Oculto", "30.00", EstadoProducto.BORRADOR, c);
        Producto inactivo = producto("CA07B", "Inactivo Oculto", "30.00", EstadoProducto.INACTIVO, c);
        Producto activo = activo("CA07C", "Activo Visible", "30.00", c);
        for (Producto p : List.of(borrador, inactivo, activo)) {
            variante(p, "M", "negro", 5, 0);
        }
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Activo Visible")));
        mvc.perform(get("/api/v1/productos").param("q", "oculto"))
                .andExpect(jsonPath("$.data.contenido", empty()));
        mvc.perform(get("/api/v1/productos/" + borrador.getSlug())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        mvc.perform(get("/api/v1/productos/" + inactivo.getSlug())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/productos/" + activo.getSlug())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-2-07 un producto sin variantes activas no aparece en el listado")
    void sinVariantesActivas() throws Exception {
        Categoria c = categoria("Sin variantes", null);
        Producto p = activo("CA07D", "Sin Variantes Activas", "30.00", c);
        Variante v = variante(p, "M", "negro", 5, 0);
        v.setActivo(false);
        variantes.saveAndFlush(v);
        sincronizar();

        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido", empty()));
    }

    @Test
    @DisplayName("CA-2-08 el detalle trae las imágenes con su color y los colores con indicador de stock")
    void detalleConImagenesPorColor() throws Exception {
        Categoria c = categoria("Detalle", null);
        Producto p = activo("CA08A", "Legging Detalle", "59.90", c);
        variante(p, "M", "negro", 5, 0);
        variante(p, "M", "rojo", 4, 0);
        imagen(p, "negro", true);
        imagen(p, "rojo", false);
        sincronizar();

        mvc.perform(get("/api/v1/productos/" + p.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Legging Detalle"))
                .andExpect(jsonPath("$.data.imagenes", hasSize(2)))
                .andExpect(jsonPath("$.data.imagenes[0].colorId").value(colorId("negro")))
                .andExpect(jsonPath("$.data.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.data.imagenes[1].colorId").value(colorId("rojo")))
                .andExpect(jsonPath("$.data.imagenes[0].urlMiniatura").exists())
                .andExpect(jsonPath("$.data.colores", hasSize(2)))
                .andExpect(jsonPath("$.data.categorias[0].nombre").value("Detalle"));
    }

    @Test
    @DisplayName("CA-2-09 el detalle indica la disponibilidad por variante, sin números exactos salvo en las últimas unidades")
    void disponibilidadPorVariante() throws Exception {
        Categoria c = categoria("Disponibilidad", null);
        Producto p = activo("CA09A", "Con Niveles", "50.00", c);
        Variante agotada = variante(p, "S", "negro", 3, 3);
        Variante ultimas = variante(p, "M", "negro", 5, 2);
        Variante disponible = variante(p, "L", "negro", 6, 0);
        Variante inactiva = variante(p, "XL", "negro", 9, 0);
        inactiva.setActivo(false);
        variantes.saveAndFlush(inactiva);
        Variante conPrecio = variante(p, "XXL", "negro", 10, 0);
        conPrecio.setPrecio(dec("64.90"));
        variantes.saveAndFlush(conPrecio);
        sincronizar();

        mvc.perform(get("/api/v1/productos/" + p.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantes", hasSize(4)))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + agotada.getId() + ")].disponibilidad").value("AGOTADO"))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + ultimas.getId() + ")].disponibilidad").value("ULTIMAS"))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + ultimas.getId() + ")].ultimasUnidades").value(3))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + disponible.getId() + ")].disponibilidad").value("DISPONIBLE"))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + disponible.getId() + ")].ultimasUnidades").doesNotExist())
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + disponible.getId() + ")].precio").value(50.0))
                .andExpect(jsonPath("$.data.variantes[?(@.id==" + conPrecio.getId() + ")].precio").value(64.9))
                .andExpect(jsonPath("$.data.tallas[*].codigo", contains("S", "M", "L", "XXL")));
    }

    @Test
    @DisplayName("CA-2-09 un color sin stock en ninguna talla viene marcado sin stock y si ninguno tiene el producto se ve agotado")
    void colorSinStock() throws Exception {
        Categoria c = categoria("Colores", null);
        Producto p = activo("CA09B", "Dos Colores", "50.00", c);
        variante(p, "S", "negro", 5, 0);
        variante(p, "M", "negro", 0, 0);
        variante(p, "S", "rojo", 2, 2);
        variante(p, "M", "rojo", 0, 0);
        Producto agotado = activo("CA09C", "Todo Agotado", "50.00", c);
        variante(agotado, "S", "negro", 0, 0);
        sincronizar();

        mvc.perform(get("/api/v1/productos/" + p.getSlug()))
                .andExpect(jsonPath("$.data.colores[?(@.nombre=='negro')].conStock").value(true))
                .andExpect(jsonPath("$.data.colores[?(@.nombre=='rojo')].conStock").value(false));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()).param("sort", "nombre"))
                .andExpect(jsonPath("$.data.contenido[0].nombre").value("Dos Colores"))
                .andExpect(jsonPath("$.data.contenido[0].agotado").value(false))
                .andExpect(jsonPath("$.data.contenido[1].nombre").value("Todo Agotado"))
                .andExpect(jsonPath("$.data.contenido[1].agotado").value(true));
    }

    @Test
    @DisplayName("CA-2-10 la guía de tallas es la de la categoría o, si no tiene, la de su padre; sin ninguna responde 404")
    void guiaDeTallasHeredada() throws Exception {
        Categoria padre = categoria("Con guía", null);
        Categoria hija = categoria("Hija sin guía", padre.getId());
        Categoria suelta = categoria("Sin guía", null);
        GuiaTallas g = new GuiaTallas();
        g.setCategoriaId(padre.getId());
        g.setTabla(Map.of("columnas", List.of("Talla", "Cintura (cm)"), "filas", List.of(List.of("S", "64-68"))));
        g.setNota("Medidas del cuerpo");
        guias.saveAndFlush(g);
        Producto p = activo("CA10A", "Con Guía Heredada", "50.00", hija);
        variante(p, "M", "negro", 5, 0);
        Producto sin = activo("CA10B", "Sin Guía", "50.00", suelta);
        variante(sin, "M", "negro", 5, 0);
        sincronizar();

        mvc.perform(get("/api/v1/categorias/" + padre.getSlug() + "/guia-tallas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.columnas", contains("Talla", "Cintura (cm)")))
                .andExpect(jsonPath("$.data.filas[0]", contains("S", "64-68")))
                .andExpect(jsonPath("$.data.nota").value("Medidas del cuerpo"));
        mvc.perform(get("/api/v1/categorias/" + hija.getSlug() + "/guia-tallas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.columnas", hasSize(2)));
        mvc.perform(get("/api/v1/categorias/" + suelta.getSlug() + "/guia-tallas")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/productos/" + p.getSlug())).andExpect(jsonPath("$.data.tieneGuiaTallas").value(true));
        mvc.perform(get("/api/v1/productos/" + sin.getSlug())).andExpect(jsonPath("$.data.tieneGuiaTallas").value(false));
    }

    @Test
    @DisplayName("CA-2-11 el detalle trae los tramos de descuento vigentes del producto")
    void tramosEnElDetalle() throws Exception {
        Categoria c = categoria("Tramos", null);
        Producto global = activo("CA11A", "Descuento Global", "50.00", c);
        variante(global, "M", "negro", 5, 0);
        Producto sin = activo("CA11B", "Sin Descuento", "50.00", c);
        sin.setModoDescuento(ModoDescuento.SIN_DESCUENTO);
        productos.saveAndFlush(sin);
        variante(sin, "M", "negro", 5, 0);
        sincronizar();

        mvc.perform(get("/api/v1/productos/" + global.getSlug()))
                .andExpect(jsonPath("$.data.tramos", hasSize(3)))
                .andExpect(jsonPath("$.data.tramos[0].unidadesMinimas").value(3))
                .andExpect(jsonPath("$.data.tramos[0].porcentaje").value(5))
                .andExpect(jsonPath("$.data.tramos[2].unidadesMinimas").value(12))
                .andExpect(jsonPath("$.data.tramos[2].porcentaje").value(15));
        mvc.perform(get("/api/v1/productos/" + sin.getSlug()))
                .andExpect(jsonPath("$.data.tramos", empty()));
    }

    @Test
    @DisplayName("CA-2-01 el árbol de categorías trae solo las activas, con sus hijas, en su orden")
    void arbolDeCategorias() throws Exception {
        Categoria raiz = categoria("Arbol raíz", null);
        Categoria hijaActiva = categoria("Arbol hija activa", raiz.getId());
        Categoria hijaInactiva = categoria("Arbol hija inactiva", raiz.getId());
        hijaInactiva.setActivo(false);
        categorias.saveAndFlush(hijaInactiva);
        Categoria inactiva = categoria("Arbol raíz inactiva", null);
        inactiva.setActivo(false);
        categorias.saveAndFlush(inactiva);
        sincronizar();

        mvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug=='" + raiz.getSlug() + "')].hijas[*].nombre").value(contains("Arbol hija activa")))
                .andExpect(jsonPath("$.data[?(@.slug=='" + inactiva.getSlug() + "')]", empty()));
    }

    @Test
    @DisplayName("CA-2-01 los filtros de tallas y colores traen solo los activos en su orden")
    void tallasYColoresPublicos() throws Exception {
        mvc.perform(get("/api/v1/tallas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].codigo", contains("XS", "S", "M", "L", "XL", "XXL")));
        mvc.perform(get("/api/v1/colores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.nombre=='negro')].hex").value("#1D1D1F"));
    }

    @Test
    @DisplayName("CA-2-27 los destacados salen en su orden, solo ACTIVO, con un máximo de 8")
    void destacados() throws Exception {
        Categoria c = categoria("Destacados", null);
        Producto segundo = activo("CA27A", "Segundo", "50.00", c);
        Producto primero = activo("CA27B", "Primero", "50.00", c);
        Producto borrador = producto("CA27C", "Borrador Destacado", "50.00", EstadoProducto.BORRADOR, c);
        Producto normal = activo("CA27D", "No Destacado", "50.00", c);
        segundo.setDestacado(true);
        segundo.setOrdenDestacado(2);
        primero.setDestacado(true);
        primero.setOrdenDestacado(1);
        borrador.setDestacado(true);
        borrador.setOrdenDestacado(0);
        productos.saveAllAndFlush(List.of(segundo, primero, borrador));
        for (Producto p : List.of(segundo, primero, borrador, normal)) {
            variante(p, "M", "negro", 5, 0);
        }
        sincronizar();

        mvc.perform(get("/api/v1/productos/destacados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].nombre", contains("Primero", "Segundo")));
    }

    @Test
    @DisplayName("CA-2-07 un slug inexistente responde 404 con el código NO_ENCONTRADO")
    void slugInexistente() throws Exception {
        mvc.perform(get("/api/v1/productos/no-existe-este-producto"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
