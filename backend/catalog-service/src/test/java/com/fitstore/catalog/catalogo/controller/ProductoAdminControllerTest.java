package com.fitstore.catalog.catalogo.controller;

import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Productos y variantes del backoffice (HU-2-05) y descuentos por producto (CA-2-38). */
class ProductoAdminControllerTest extends ApiBase {

    private static final String BASE = "/api/v1/admin/productos";

    private ResultActions crear(String codigo, String nombre, String precio, Long... categoriaIds) throws Exception {
        String cats = java.util.Arrays.stream(categoriaIds).map(String::valueOf).collect(Collectors.joining(","));
        return mvc.perform(post(BASE).with(admin()).contentType(APPLICATION_JSON).content(
                "{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre + "\",\"descripcion\":\"Tela de compresión\","
                        + "\"precioBase\":" + precio + ",\"categoriaIds\":[" + cats + "]}"));
    }

    private long idProducto(ResultActions r) throws Exception {
        return cuerpo(r).path("data").path("id").asLong();
    }

    private ResultActions matriz(long productoId, List<Long> tallaIds, List<Long> colorIds) throws Exception {
        return mvc.perform(post(BASE + "/" + productoId + "/variantes/matriz").with(admin()).contentType(APPLICATION_JSON)
                .content("{\"tallaIds\":" + tallaIds + ",\"colorIds\":" + colorIds + "}"));
    }

    private JsonNode producto(long id) throws Exception {
        return cuerpo(mvc.perform(get(BASE + "/" + id).with(admin())).andExpect(status().isOk())).path("data");
    }

    // ------------------------------------------------------------ CA-2-21

    @Test
    @DisplayName("CA-2-21 al crear un producto queda en BORRADOR con slug generado y código único")
    void creaEnBorrador() throws Exception {
        Categoria c = categoria("Crear", null);
        crear("LEG01", "Legging Pro Compresión", "59.90", c.getId()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.data.slug").value("legging-pro-compresion"))
                .andExpect(jsonPath("$.data.codigo").value("LEG01"))
                .andExpect(jsonPath("$.data.precioBase").value(59.9))
                .andExpect(jsonPath("$.data.categoriaIds", contains(c.getId().intValue())))
                .andExpect(jsonPath("$.data.modoDescuento").value("GLOBAL"))
                .andExpect(jsonPath("$.data.destacado").value(false))
                .andExpect(jsonPath("$.data.variantes", empty()))
                .andExpect(jsonPath("$.data.version").value(0));
        // mismo nombre, otro código: el slug se vuelve único
        crear("LEG02", "Legging Pro Compresión", "59.90").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("legging-pro-compresion-2"));
    }

    @Test
    @DisplayName("CA-2-21 rechaza un código repetido con 409 y datos inválidos con 400")
    void validaCodigoYDatos() throws Exception {
        crear("DUP01", "Primero", "30.00").andExpect(status().isCreated());
        crear("DUP01", "Segundo", "30.00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));
        crear("minusc", "Código en minúsculas", "30.00").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        crear("PREC01", "Precio negativo", "-1").andExpect(status().isBadRequest());
        crear("CAT01", "Categoría inexistente", "30.00", 999999999L).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-21 edita los datos del producto con la versión; una versión vieja responde 409")
    void editaConVersion() throws Exception {
        long id = idProducto(crear("EDI01", "Producto editable", "40.00"));

        mvc.perform(put(BASE + "/" + id).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"EDI01\",\"nombre\":\"Producto editado\",\"descripcion\":\"Nueva\",\"precioBase\":45.5,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Producto editado"))
                .andExpect(jsonPath("$.data.precioBase").value(45.5))
                .andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(put(BASE + "/" + id).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"EDI01\",\"nombre\":\"Otro intento\",\"descripcion\":\"x\",\"precioBase\":45.5,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        mvc.perform(put(BASE + "/" + id).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"EDI01\",\"nombre\":\"Sin versión\",\"descripcion\":\"x\",\"precioBase\":45.5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(BASE + "/999999999").with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"EDI02\",\"nombre\":\"No existe\",\"descripcion\":\"x\",\"precioBase\":45.5,\"version\":0}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ CA-2-22

    @Test
    @DisplayName("CA-2-22 la matriz de 5 tallas y 3 colores crea 15 variantes con SKU {código}-{COL}-{TALLA} y stock físico 0")
    void generaLaMatriz() throws Exception {
        long id = idProducto(crear("LEG01", "Legging Matriz", "59.90"));
        List<Long> tallasIds = List.of(tallaId("XS"), tallaId("S"), tallaId("M"), tallaId("L"), tallaId("XL"));
        List<Long> coloresIds = List.of(colorId("negro"), colorId("rojo"), colorId("blanco"));

        matriz(id, tallasIds, coloresIds).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes", hasSize(15)))
                .andExpect(jsonPath("$.data.variantes[*].sku", hasItem("LEG01-NEG-M")))
                .andExpect(jsonPath("$.data.variantes[*].sku", hasItem("LEG01-ROJ-XL")))
                .andExpect(jsonPath("$.data.variantes[*].sku", hasItem("LEG01-BLA-XS")))
                .andExpect(jsonPath("$.data.variantes[?(@.sku=='LEG01-NEG-M')].fisico").value(0))
                .andExpect(jsonPath("$.data.variantes[?(@.sku=='LEG01-NEG-M')].activo").value(true));
        sincronizar();
        Integer filasDeStock = jdbc.queryForObject(
                "SELECT count(*) FROM stock s JOIN variante v ON v.id = s.variante_id WHERE v.producto_id = ? AND s.fisico = 0 AND s.reservado = 0",
                Integer.class, id);
        assertThat(filasDeStock).isEqualTo(15);
    }

    @Test
    @DisplayName("CA-2-22 al repetir la matriz omite las variantes que ya existían")
    void omiteLasExistentes() throws Exception {
        long id = idProducto(crear("LEG03", "Legging Repetido", "59.90"));
        matriz(id, List.of(tallaId("S"), tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes", hasSize(2)));
        Long primera = producto(id).path("variantes").get(0).path("id").asLong();

        matriz(id, List.of(tallaId("S"), tallaId("M"), tallaId("L")), List.of(colorId("negro"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes", hasSize(3)));
        assertThat(producto(id).path("variantes").get(0).path("id").asLong()).isEqualTo(primera);
        matriz(id, List.of(tallaId("S")), List.of(999999999L)).andExpect(status().isNotFound());
        matriz(id, List.of(), List.of(colorId("negro"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-22 si dos colores comparten las 3 primeras letras, el segundo lleva un dígito en el SKU")
    void codigosDeColorQueChocan() throws Exception {
        long id = idProducto(crear("AZU01", "Legging Azules", "59.90"));

        matriz(id, List.of(tallaId("M")), List.of(colorId("azul"), colorId("azul marino"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes[*].sku", containsInAnyOrder("AZU01-AZU-M", "AZU01-AZU2-M")));
    }

    @Test
    @DisplayName("CA-2-22 crea una variante suelta con SKU sugerido o propio, y rechaza combinación o SKU repetidos")
    void variantesSueltas() throws Exception {
        long id = idProducto(crear("VAR01", "Con variante suelta", "40.00"));
        String sugerido = "{\"tallaId\":" + tallaId("M") + ",\"colorId\":" + colorId("negro") + "}";

        mvc.perform(post(BASE + "/" + id + "/variantes").with(admin()).contentType(APPLICATION_JSON).content(sugerido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes[0].sku").value("VAR01-NEG-M"));
        mvc.perform(post(BASE + "/" + id + "/variantes").with(admin()).contentType(APPLICATION_JSON).content(sugerido))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("VARIANTE_DUPLICADA"));
        mvc.perform(post(BASE + "/" + id + "/variantes").with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"tallaId\":" + tallaId("L") + ",\"colorId\":" + colorId("negro") + ",\"sku\":\"VAR01-NEG-M\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SKU_DUPLICADO"));
        mvc.perform(post(BASE + "/" + id + "/variantes").with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"tallaId\":" + tallaId("L") + ",\"colorId\":" + colorId("negro") + ",\"sku\":\"MI-SKU-L\",\"precio\":44.9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantes[?(@.sku=='MI-SKU-L')].precio").value(44.9));
    }

    // ------------------------------------------------------------ CA-2-23

    @Test
    @DisplayName("CA-2-23 'desde' se recalcula con el menor precio de las variantes activas al cambiar precios, base o estado")
    void recalculaPrecioDesde() throws Exception {
        long id = idProducto(crear("PRE01", "Precio desde", "50.00"));
        matriz(id, List.of(tallaId("S"), tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated());
        JsonNode vars = producto(id).path("variantes");
        long varS = vars.get(0).path("id").asLong();
        long varM = vars.get(1).path("id").asLong();
        assertThat(productos.findById(id).orElseThrow().getPrecioDesde()).isEqualByComparingTo("50.00");

        mvc.perform(put("/api/v1/admin/variantes/" + varS).with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"PRE01-NEG-S\",\"precio\":42.5}"))
                .andExpect(status().isOk());
        sincronizar();
        assertThat(productos.findById(id).orElseThrow().getPrecioDesde()).isEqualByComparingTo("42.50");

        mvc.perform(put(BASE + "/" + id).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"codigo\":\"PRE01\",\"nombre\":\"Precio desde\",\"descripcion\":\"x\",\"precioBase\":30,\"version\":" + producto(id).path("version").asInt() + "}"))
                .andExpect(status().isOk());
        sincronizar();
        assertThat(productos.findById(id).orElseThrow().getPrecioDesde()).isEqualByComparingTo("30.00");

        // la variante barata se desactiva: el "desde" ya no la cuenta
        mvc.perform(patch("/api/v1/admin/variantes/" + varM + "/desactivar").with(admin())).andExpect(status().isOk());
        sincronizar();
        assertThat(productos.findById(id).orElseThrow().getPrecioDesde()).isEqualByComparingTo("42.50");
        mvc.perform(patch("/api/v1/admin/variantes/" + varM + "/activar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.variantes[?(@.id==" + varM + ")].activo").value(true));
    }

    // ------------------------------------------------------------ CA-2-24 y 25

    @Test
    @DisplayName("CA-2-24 publicar sin categoría, sin variante activa, con precio 0 o con un color sin foto responde 409 con la lista de faltantes")
    void publicarIncompleto() throws Exception {
        long vacio = idProducto(crear("PUB01", "Producto vacío", "0"));
        mvc.perform(patch(BASE + "/" + vacio + "/publicar").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NO_PUBLICABLE"))
                .andExpect(jsonPath("$.detalles.faltantes", hasSize(3)))
                .andExpect(jsonPath("$.detalles.faltantes", hasItem("Asigna al menos una categoría")))
                .andExpect(jsonPath("$.detalles.faltantes", hasItem("Define un precio mayor a 0")))
                .andExpect(jsonPath("$.detalles.faltantes", hasItem("Agrega al menos una variante activa")));

        Categoria c = categoria("Publicar", null);
        long sinFoto = idProducto(crear("PUB02", "Sin foto de rojo", "50.00", c.getId()));
        matriz(sinFoto, List.of(tallaId("M")), List.of(colorId("negro"), colorId("rojo"))).andExpect(status().isCreated());
        Producto entidad = productos.findById(sinFoto).orElseThrow();
        imagen(entidad, "negro", true);
        sincronizar();

        mvc.perform(patch(BASE + "/" + sinFoto + "/publicar").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.faltantes", contains("Agrega una imagen del color rojo")));
        assertThat(productos.findById(sinFoto).orElseThrow().getEstado()).isEqualTo(EstadoProducto.BORRADOR);
        mvc.perform(patch(BASE + "/999999999/publicar").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-25 al publicar un producto completo pasa a ACTIVO y aparece en la tienda; al desactivarlo deja de aparecer")
    void publicarYDesactivar() throws Exception {
        Categoria c = categoria("Publicado", null);
        long id = idProducto(crear("PUB03", "Producto completo", "50.00", c.getId()));
        matriz(id, List.of(tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated());
        Producto entidad = productos.findById(id).orElseThrow();
        imagen(entidad, "negro", true);
        Variante v = variantes.findByProductoIdOrderByIdAsc(id).get(0);
        stock.saveAndFlush(stockCon(v.getId(), 5));
        sincronizar();
        mvc.perform(get("/api/v1/productos/" + entidad.getSlug())).andExpect(status().isNotFound());

        mvc.perform(patch(BASE + "/" + id + "/publicar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("ACTIVO"));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido[*].nombre", contains("Producto completo")));
        mvc.perform(get("/api/v1/productos/" + entidad.getSlug())).andExpect(status().isOk());

        mvc.perform(patch(BASE + "/" + id + "/desactivar").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("INACTIVO"));
        mvc.perform(get("/api/v1/productos").param("categoria", c.getSlug()))
                .andExpect(jsonPath("$.data.contenido", empty()));
        mvc.perform(get("/api/v1/productos/" + entidad.getSlug())).andExpect(status().isNotFound());
        // se vuelve a publicar desde INACTIVO
        mvc.perform(patch(BASE + "/" + id + "/publicar").with(admin())).andExpect(status().isOk());
    }

    private com.fitstore.catalog.inventario.model.Stock stockCon(Long varianteId, int fisico) {
        com.fitstore.catalog.inventario.model.Stock s = stock.findById(varianteId).orElseThrow();
        s.setFisico(fisico);
        return s;
    }

    @Test
    @DisplayName("CA-2-25 un producto en BORRADOR no se puede desactivar")
    void desactivarBorrador() throws Exception {
        long id = idProducto(crear("PUB04", "Borrador", "50.00"));

        mvc.perform(patch(BASE + "/" + id + "/desactivar").with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
    }

    // ------------------------------------------------------------ CA-2-26

    @Test
    @DisplayName("CA-2-26 elimina un producto en BORRADOR sin movimientos con sus variantes, stock e imágenes")
    void eliminaBorradorLimpio() throws Exception {
        long id = idProducto(crear("DEL01", "Para borrar", "50.00"));
        matriz(id, List.of(tallaId("S"), tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated());
        imagen(productos.findById(id).orElseThrow(), "negro", true);
        sincronizar();

        mvc.perform(delete(BASE + "/" + id).with(admin())).andExpect(status().isOk());
        sincronizar();
        assertThat(productos.findById(id)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM variante WHERE producto_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen WHERE producto_id = ?", Integer.class, id)).isZero();
        mvc.perform(get(BASE + "/" + id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-26 un producto con movimientos, con reservas o fuera de BORRADOR no se elimina: responde 409")
    void noEliminaSiTieneHistorial() throws Exception {
        long conMovimiento = idProducto(crear("DEL02", "Con movimiento", "50.00"));
        matriz(conMovimiento, List.of(tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated());
        Long varianteId = variantes.findByProductoIdOrderByIdAsc(conMovimiento).get(0).getId();
        jdbc.update("INSERT INTO movimiento_stock (variante_id, tipo, cantidad, saldo_fisico, creado_en) VALUES (?, 'ENTRADA', 5, 5, now())", varianteId);

        long inactivo = idProducto(crear("DEL03", "Ya publicado", "50.00"));
        Producto entidad = productos.findById(inactivo).orElseThrow();
        entidad.setEstado(EstadoProducto.INACTIVO);
        productos.saveAndFlush(entidad);
        sincronizar();

        mvc.perform(delete(BASE + "/" + conMovimiento).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ELIMINABLE"));
        mvc.perform(delete(BASE + "/" + inactivo).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ELIMINABLE"));
        assertThat(productos.findById(conMovimiento)).isPresent();
        mvc.perform(delete(BASE + "/999999999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-26 lo mismo para una variante: sin movimientos ni reservas se elimina; si no, 409 y se desactiva")
    void eliminarVariante() throws Exception {
        long id = idProducto(crear("DEL04", "Variantes a borrar", "50.00"));
        matriz(id, List.of(tallaId("S"), tallaId("M")), List.of(colorId("negro"))).andExpect(status().isCreated());
        List<Variante> vs = variantes.findByProductoIdOrderByIdAsc(id);
        Long libre = vs.get(0).getId();
        Long conReserva = vs.get(1).getId();
        jdbc.update("INSERT INTO reserva (pedido_id, estado, expira_en, creado_en, actualizado_en) VALUES ('9f2c6a1e-0000-4000-8000-000000000001', 'ACTIVA', now(), now(), now())");
        jdbc.update("UPDATE stock SET fisico = 3, reservado = 1 WHERE variante_id = ?", conReserva);
        jdbc.update("INSERT INTO reserva_item (pedido_id, variante_id, cantidad) VALUES ('9f2c6a1e-0000-4000-8000-000000000001', ?, 1)", conReserva);
        sincronizar();

        mvc.perform(delete("/api/v1/admin/variantes/" + conReserva).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("VARIANTE_NO_ELIMINABLE"));
        mvc.perform(delete("/api/v1/admin/variantes/" + libre).with(admin())).andExpect(status().isOk());
        sincronizar();
        assertThat(variantes.findById(libre)).isEmpty();
        assertThat(stock.findById(libre)).isEmpty();
        mvc.perform(delete("/api/v1/admin/variantes/" + libre).with(admin())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/admin/variantes/" + conReserva + "/desactivar").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantes[0].activo").value(false));
    }

    // ------------------------------------------------------------ CA-2-27

    @Test
    @DisplayName("CA-2-27 marcar un producto como destacado con orden lo muestra en los destacados en ese orden")
    void destacar() throws Exception {
        Categoria c = categoria("Destacar", null);
        Producto a = producto("DES01", "Destacado A", "50.00", EstadoProducto.ACTIVO, c);
        Producto b = producto("DES02", "Destacado B", "50.00", EstadoProducto.ACTIVO, c);
        variante(a, "M", "negro", 5, 0);
        variante(b, "M", "negro", 5, 0);
        sincronizar();

        mvc.perform(patch(BASE + "/" + a.getId() + "/destacado").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"destacado\":true,\"orden\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destacado").value(true)).andExpect(jsonPath("$.data.ordenDestacado").value(2));
        mvc.perform(patch(BASE + "/" + b.getId() + "/destacado").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"destacado\":true,\"orden\":1}"))
                .andExpect(status().isOk());
        sincronizar();
        mvc.perform(get("/api/v1/productos/destacados"))
                .andExpect(jsonPath("$.data[*].nombre", contains("Destacado B", "Destacado A")));

        mvc.perform(patch(BASE + "/" + b.getId() + "/destacado").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"destacado\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destacado").value(false)).andExpect(jsonPath("$.data.ordenDestacado", nullValue()));
        sincronizar();
        mvc.perform(get("/api/v1/productos/destacados")).andExpect(jsonPath("$.data[*].nombre", contains("Destacado A")));
    }

    @Test
    @DisplayName("CA-2-21 asigna las categorías de un producto reemplazando las anteriores")
    void asignaCategorias() throws Exception {
        Categoria uno = categoria("Cat uno", null);
        Categoria dos = categoria("Cat dos", null);
        long id = idProducto(crear("CAT02", "Producto categorías", "50.00", uno.getId()));

        mvc.perform(put(BASE + "/" + id + "/categorias").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"categoriaIds\":[" + dos.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoriaIds", contains(dos.getId().intValue())));
        mvc.perform(put(BASE + "/" + id + "/categorias").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"categoriaIds\":[999999999]}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ listado y lectura

    @Test
    @DisplayName("CA-2-21 el listado de administración trae todos los estados, filtra por estado, categoría, texto y destacado, y pagina de a 20")
    void listadoAdmin() throws Exception {
        Categoria c = categoria("Listado admin", null);
        Producto borrador = producto("LIS01", "Alfa borrador", "30.00", EstadoProducto.BORRADOR, c);
        Producto activo = producto("LIS02", "Beta activo", "40.00", EstadoProducto.ACTIVO, c);
        activo.setDestacado(true);
        productos.saveAndFlush(activo);
        Producto otro = producto("LIS03", "Gamma inactivo", "50.00", EstadoProducto.INACTIVO);
        variante(activo, "M", "negro", 4, 1);
        variante(activo, "S", "negro", 2, 0);
        imagen(activo, "negro", true);
        sincronizar();

        mvc.perform(get(BASE).with(admin()).param("categoriaId", c.getId().toString()).param("sort", "nombre,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tamano").value(20))
                .andExpect(jsonPath("$.data.contenido[*].codigo", contains("LIS01", "LIS02")))
                .andExpect(jsonPath("$.data.contenido[1].cantidadVariantes").value(2))
                .andExpect(jsonPath("$.data.contenido[1].stockDisponible").value(5))
                .andExpect(jsonPath("$.data.contenido[1].imagenUrl").exists());
        mvc.perform(get(BASE).with(admin()).param("estado", "INACTIVO").param("q", "gamma"))
                .andExpect(jsonPath("$.data.contenido[*].codigo", contains("LIS03")));
        mvc.perform(get(BASE).with(admin()).param("q", "lis02"))
                .andExpect(jsonPath("$.data.contenido[*].codigo", contains("LIS02")));
        mvc.perform(get(BASE).with(admin()).param("destacado", "true").param("categoriaId", c.getId().toString()))
                .andExpect(jsonPath("$.data.contenido[*].codigo", contains("LIS02")));
        assertThat(borrador.getId()).isNotEqualTo(otro.getId());
    }

    @Test
    @DisplayName("CA-2-21 el producto completo trae variantes con stock numérico, imágenes y categorías")
    void productoCompleto() throws Exception {
        Categoria c = categoria("Completo", null);
        Producto p = producto("COM01", "Producto completo admin", "50.00", EstadoProducto.BORRADOR, c);
        variante(p, "M", "negro", 9, 2);
        imagen(p, "negro", true);
        sincronizar();

        mvc.perform(get(BASE + "/" + p.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantes[0].fisico").value(9))
                .andExpect(jsonPath("$.data.variantes[0].reservado").value(2))
                .andExpect(jsonPath("$.data.variantes[0].disponible").value(7))
                .andExpect(jsonPath("$.data.variantes[0].talla").value("M"))
                .andExpect(jsonPath("$.data.variantes[0].color").value("negro"))
                .andExpect(jsonPath("$.data.imagenes", hasSize(1)))
                .andExpect(jsonPath("$.data.categoriaIds", contains(c.getId().intValue())));
    }

    // ------------------------------------------------------------ CA-2-28

    @Test
    @DisplayName("CA-2-28 el OPERADOR ve productos y variantes pero no puede crear, editar, publicar ni borrar: 403")
    void permisosDelOperador() throws Exception {
        Categoria c = categoria("Permisos", null);
        Producto p = producto("PER01", "Producto permisos", "50.00", EstadoProducto.BORRADOR, c);
        Variante v = variante(p, "M", "negro", 1, 0);
        sincronizar();
        String cuerpoProducto = "{\"codigo\":\"PER02\",\"nombre\":\"Intento\",\"descripcion\":\"x\",\"precioBase\":10,\"version\":0}";

        mvc.perform(get(BASE).with(operador())).andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + p.getId()).with(operador())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantes", hasSize(1)));
        mvc.perform(post(BASE).with(operador()).contentType(APPLICATION_JSON).content(cuerpoProducto)).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + p.getId()).with(operador()).contentType(APPLICATION_JSON).content(cuerpoProducto)).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + p.getId()).with(operador())).andExpect(status().isForbidden());
        mvc.perform(patch(BASE + "/" + p.getId() + "/publicar").with(operador())).andExpect(status().isForbidden());
        mvc.perform(patch(BASE + "/" + p.getId() + "/desactivar").with(operador())).andExpect(status().isForbidden());
        mvc.perform(patch(BASE + "/" + p.getId() + "/destacado").with(operador()).contentType(APPLICATION_JSON)
                .content("{\"destacado\":true}")).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + p.getId() + "/categorias").with(operador()).contentType(APPLICATION_JSON)
                .content("{\"categoriaIds\":[]}")).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + p.getId() + "/descuento").with(operador()).contentType(APPLICATION_JSON)
                .content("{\"modo\":\"SIN_DESCUENTO\"}")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + p.getId() + "/variantes").with(operador()).contentType(APPLICATION_JSON)
                .content("{\"tallaId\":1,\"colorId\":1}")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + p.getId() + "/variantes/matriz").with(operador()).contentType(APPLICATION_JSON)
                .content("{\"tallaIds\":[1],\"colorIds\":[1]}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/variantes/" + v.getId()).with(operador()).contentType(APPLICATION_JSON)
                .content("{\"sku\":\"X\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/variantes/" + v.getId()).with(operador())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/variantes/" + v.getId() + "/desactivar").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        assertThat(productos.findById(p.getId())).isPresent();
    }

    // ------------------------------------------------------------ CA-2-38

    @Test
    @DisplayName("CA-2-38 un producto PERSONALIZADO cotiza con sus tramos y uno SIN_DESCUENTO sin ninguno, sin afectar a los demás")
    void descuentoPorProducto() throws Exception {
        Categoria c = categoria("Descuento propio", null);
        Producto propio = producto("DSC01", "Con tramos propios", "50.00", EstadoProducto.ACTIVO, c);
        Producto sin = producto("DSC02", "Sin descuento", "50.00", EstadoProducto.ACTIVO, c);
        Producto global = producto("DSC03", "Con política global", "50.00", EstadoProducto.ACTIVO, c);
        Variante vp = variante(propio, "M", "negro", 50, 0);
        Variante vs = variante(sin, "M", "negro", 50, 0);
        Variante vg = variante(global, "M", "negro", 50, 0);
        sincronizar();

        mvc.perform(put(BASE + "/" + propio.getId() + "/descuento").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"modo\":\"PERSONALIZADO\",\"tramos\":[{\"unidadesMinimas\":2,\"porcentaje\":20}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modoDescuento").value("PERSONALIZADO"))
                .andExpect(jsonPath("$.data.tramosPropios", hasSize(1)))
                .andExpect(jsonPath("$.data.tramosPropios[0].porcentaje").value(20));
        mvc.perform(put(BASE + "/" + sin.getId() + "/descuento").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"modo\":\"SIN_DESCUENTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.modoDescuento").value("SIN_DESCUENTO"));
        sincronizar();

        String cotizacion = "{\"lineas\":[{\"varianteId\":" + vp.getId() + ",\"cantidad\":2},{\"varianteId\":" + vs.getId()
                + ",\"cantidad\":12},{\"varianteId\":" + vg.getId() + ",\"cantidad\":3}]}";
        mvc.perform(post("/api/v1/precios/cotizar").contentType(APPLICATION_JSON).content(cotizacion))
                .andExpect(jsonPath("$.data.lineas[0].descuentoPorcentaje").value(20))
                .andExpect(jsonPath("$.data.lineas[1].descuentoPorcentaje").value(0))
                .andExpect(jsonPath("$.data.lineas[2].descuentoPorcentaje").value(5));
        mvc.perform(get("/api/v1/productos/" + propio.getSlug()))
                .andExpect(jsonPath("$.data.tramos", hasSize(1)))
                .andExpect(jsonPath("$.data.tramos[0].unidadesMinimas").value(2));

        // volver a GLOBAL borra los tramos propios
        mvc.perform(put(BASE + "/" + propio.getId() + "/descuento").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"modo\":\"GLOBAL\",\"tramos\":[{\"unidadesMinimas\":2,\"porcentaje\":20}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modoDescuento").value("GLOBAL"))
                .andExpect(jsonPath("$.data.tramosPropios", empty()));
        sincronizar();
        assertThat(tramos.findByProductoIdOrderByUnidadesMinimasAsc(propio.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-2-38 el modo PERSONALIZADO exige tramos válidos: sin tramos o con tramos que no crecen responde 400")
    void descuentoPersonalizadoInvalido() throws Exception {
        Producto p = producto("DSC04", "Tramos inválidos", "50.00", EstadoProducto.BORRADOR);
        sincronizar();
        String url = BASE + "/" + p.getId() + "/descuento";

        mvc.perform(put(url).with(admin()).contentType(APPLICATION_JSON).content("{\"modo\":\"PERSONALIZADO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(url).with(admin()).contentType(APPLICATION_JSON).content("{\"modo\":\"PERSONALIZADO\",\"tramos\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(url).with(admin()).contentType(APPLICATION_JSON).content(
                        "{\"modo\":\"PERSONALIZADO\",\"tramos\":[{\"unidadesMinimas\":5,\"porcentaje\":10},{\"unidadesMinimas\":3,\"porcentaje\":20}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(BASE + "/999999999/descuento").with(admin()).contentType(APPLICATION_JSON).content("{\"modo\":\"GLOBAL\"}"))
                .andExpect(status().isNotFound());
        assertThat(productos.findById(p.getId()).orElseThrow().getModoDescuento().name()).isEqualTo("GLOBAL");
    }
}
