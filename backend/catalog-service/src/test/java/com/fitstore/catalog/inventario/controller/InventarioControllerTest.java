package com.fitstore.catalog.inventario.controller;

import static com.fitstore.catalog.pruebas.Sesiones.ADMIN_ID;
import static com.fitstore.catalog.pruebas.Sesiones.OPERADOR_ID;
import static com.fitstore.catalog.pruebas.Sesiones.admin;
import static com.fitstore.catalog.pruebas.Sesiones.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Control del stock: entradas, ajustes, kardex, mínimos, alerta de stock bajo y filtros (HU-2-09). */
class InventarioControllerTest extends ApiBase {

    private static final String BASE = "/api/v1/admin/inventario";

    private Variante nueva(String codigo, String talla, String color, int fisico, int reservado) {
        Categoria c = categoria("Inventario " + codigo, null);
        Producto p = producto(codigo, "Producto " + codigo, "50.00", EstadoProducto.ACTIVO, c);
        return variante(p, talla, color, fisico, reservado);
    }

    private ResultActions entrada(String json) throws Exception {
        return mvc.perform(post(BASE + "/entradas").with(admin()).contentType(APPLICATION_JSON).content(json));
    }

    private ResultActions ajuste(long varianteId, int cantidad, String motivo) throws Exception {
        String m = motivo == null ? "" : ",\"motivo\":\"" + motivo + "\"";
        return mvc.perform(post(BASE + "/ajustes").with(admin()).contentType(APPLICATION_JSON)
                .content("{\"varianteId\":" + varianteId + ",\"cantidad\":" + cantidad + m + "}"));
    }

    private int cuantasAlertas(long varianteId) {
        em.flush(); // el outbox se guarda con JPA: se manda a la base antes de leer con SQL
        return jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'StockBajo' AND agregado_id = ?",
                Integer.class, String.valueOf(varianteId));
    }

    private List<Map<String, Object>> movimientos(long varianteId) {
        return jdbc.queryForList("SELECT * FROM movimiento_stock WHERE variante_id = ? ORDER BY id", varianteId);
    }

    // ------------------------------------------------------------ CA-2-39

    @Test
    @DisplayName("CA-2-39 una entrada de varias líneas sube el físico de cada SKU y deja un movimiento ENTRADA por línea con usuario y motivo")
    void entradaDeVariasLineas() throws Exception {
        Variante a = nueva("INV01", "M", "negro", 2, 0);
        Variante b = nueva("INV02", "S", "rojo", 0, 0);
        sincronizar();

        entrada("{\"lineas\":[{\"varianteId\":" + a.getId() + ",\"cantidad\":10},{\"varianteId\":" + b.getId()
                + ",\"cantidad\":4}],\"motivo\":\"Lote de producción 15/10\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.varianteId==" + a.getId() + ")].fisico").value(12))
                .andExpect(jsonPath("$.data[?(@.varianteId==" + b.getId() + ")].fisico").value(4))
                .andExpect(jsonPath("$.data[?(@.varianteId==" + b.getId() + ")].disponible").value(4));

        List<Map<String, Object>> deA = movimientos(a.getId());
        assertThat(deA).hasSize(1);
        assertThat(deA.get(0)).containsEntry("tipo", "ENTRADA").containsEntry("cantidad", 10).containsEntry("saldo_fisico", 12)
                .containsEntry("motivo", "Lote de producción 15/10");
        assertThat(deA.get(0).get("usuario_id").toString()).isEqualTo(ADMIN_ID);
        assertThat(movimientos(b.getId())).hasSize(1);
    }

    @Test
    @DisplayName("CA-2-39 una variante inexistente responde 404 y no cambia nada; el OPERADOR también puede registrar entradas")
    void entradaTodoONada() throws Exception {
        Variante a = nueva("INV03", "M", "negro", 2, 0);
        sincronizar();

        entrada("{\"lineas\":[{\"varianteId\":" + a.getId() + ",\"cantidad\":10},{\"varianteId\":999999999,\"cantidad\":1}]}")
                .andExpect(status().isNotFound());
        entrada("{\"lineas\":[]}").andExpect(status().isBadRequest());
        entrada("{\"lineas\":[{\"varianteId\":" + a.getId() + ",\"cantidad\":0}]}").andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT fisico FROM stock WHERE variante_id = ?", Integer.class, a.getId())).isEqualTo(2);
        assertThat(movimientos(a.getId())).isEmpty();

        mvc.perform(post(BASE + "/entradas").with(operador()).contentType(APPLICATION_JSON)
                        .content("{\"lineas\":[{\"varianteId\":" + a.getId() + ",\"cantidad\":3}]}"))
                .andExpect(status().isCreated());
        assertThat(movimientos(a.getId()).get(0).get("usuario_id").toString()).isEqualTo(OPERADOR_ID);
    }

    // ------------------------------------------------------------ CA-2-40

    @Test
    @DisplayName("CA-2-40 un ajuste positivo o negativo con motivo cambia el físico y queda como movimiento AJUSTE con signo")
    void ajusteValido() throws Exception {
        Variante v = nueva("INV04", "M", "negro", 10, 2);
        sincronizar();

        ajuste(v.getId(), -3, "Merma").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fisico").value(7)).andExpect(jsonPath("$.data.disponible").value(5));
        ajuste(v.getId(), 4, "Conteo físico").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fisico").value(11));

        List<Map<String, Object>> lista = movimientos(v.getId());
        assertThat(lista).extracting(m -> m.get("tipo")).containsExactly("AJUSTE", "AJUSTE");
        assertThat(lista).extracting(m -> m.get("cantidad")).containsExactly(-3, 4);
        assertThat(lista).extracting(m -> m.get("saldo_fisico")).containsExactly(7, 11);
    }

    @Test
    @DisplayName("CA-2-40 un ajuste sin motivo responde 400 y uno que dejaría el físico por debajo de lo reservado responde 409; no cambia nada")
    void ajusteRechazado() throws Exception {
        Variante v = nueva("INV05", "M", "negro", 5, 3);
        sincronizar();

        ajuste(v.getId(), -1, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        ajuste(v.getId(), -1, "  ").andExpect(status().isBadRequest());
        ajuste(v.getId(), 0, "Sin efecto").andExpect(status().isBadRequest());
        ajuste(v.getId(), -3, "Demasiado").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AJUSTE_BAJO_RESERVADO"));
        ajuste(v.getId(), -9, "Negativo").andExpect(status().isConflict());
        ajuste(999999999L, 1, "No existe").andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT fisico FROM stock WHERE variante_id = ?", Integer.class, v.getId())).isEqualTo(5);
        assertThat(movimientos(v.getId())).isEmpty();
        // justo hasta lo reservado sí se puede
        ajuste(v.getId(), -2, "Hasta lo reservado").andExpect(status().isCreated()).andExpect(jsonPath("$.data.disponible").value(0));
    }

    // ------------------------------------------------------------ CA-2-41

    @Test
    @DisplayName("CA-2-41 el kardex trae el stock actual y los movimientos paginados, del más reciente al más antiguo")
    void kardex() throws Exception {
        Variante v = nueva("INV06", "M", "negro", 0, 0);
        sincronizar();
        for (int i = 1; i <= 5; i++) {
            entrada("{\"lineas\":[{\"varianteId\":" + v.getId() + ",\"cantidad\":" + i + "}],\"motivo\":\"Entrada " + i + "\"}")
                    .andExpect(status().isCreated());
        }
        jdbc.update("INSERT INTO movimiento_stock (variante_id, tipo, cantidad, saldo_fisico, referencia, creado_en) "
                + "VALUES (?, 'VENTA', -2, 13, 'FS-000123', now() + interval '1 minute')", v.getId());

        mvc.perform(get(BASE + "/" + v.getId() + "/movimientos").with(operador()).param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock.sku").value(v.getSku()))
                .andExpect(jsonPath("$.data.stock.fisico").value(15))
                .andExpect(jsonPath("$.data.movimientos.tamano").value(4))
                .andExpect(jsonPath("$.data.movimientos.totalElementos").value(6))
                .andExpect(jsonPath("$.data.movimientos.totalPaginas").value(2))
                .andExpect(jsonPath("$.data.movimientos.contenido", hasSize(4)))
                .andExpect(jsonPath("$.data.movimientos.contenido[0].tipo").value("VENTA"))
                .andExpect(jsonPath("$.data.movimientos.contenido[0].referencia").value("FS-000123"))
                .andExpect(jsonPath("$.data.movimientos.contenido[0].usuarioNombre", nullValue()))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].cantidad").value(5))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].saldoFisico").value(15))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].motivo").value("Entrada 5"))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].usuarioNombre").value(ADMIN_ID))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].creadoEn").exists());
        mvc.perform(get(BASE + "/" + v.getId() + "/movimientos").with(operador()).param("size", "4").param("page", "1"))
                .andExpect(jsonPath("$.data.movimientos.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.movimientos.contenido[1].motivo").value("Entrada 1"));
        mvc.perform(get(BASE + "/999999999/movimientos").with(operador())).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ CA-2-42

    @Test
    @DisplayName("CA-2-42 con mínimo 5 y disponible de 6 a 4 se publica StockBajo una vez; no se repite hasta superar el mínimo")
    void stockBajoSoloAlCruzar() throws Exception {
        Variante v = nueva("INV07", "M", "negro", 6, 0);
        jdbc.update("UPDATE stock SET minimo = 5 WHERE variante_id = ?", v.getId());
        sincronizar();

        ajuste(v.getId(), -2, "Venta en tienda").andExpect(status().isCreated());
        assertThat(cuantasAlertas(v.getId())).isEqualTo(1);
        em.flush();
        String payload = jdbc.queryForObject("SELECT payload::text FROM outbox_evento WHERE tipo = 'StockBajo' AND agregado_id = ?",
                String.class, String.valueOf(v.getId()));
        assertThat(payload).contains("\"sku\"", "\"disponible\"").contains("\"disponible\": 4").contains("\"minimo\": 5")
                .contains("\"producto\": \"Producto INV07\"").contains("\"talla\": \"M\"").contains("\"color\": \"negro\"");

        ajuste(v.getId(), -1, "Otra merma").andExpect(status().isCreated());
        assertThat(cuantasAlertas(v.getId())).isEqualTo(1);

        entrada("{\"lineas\":[{\"varianteId\":" + v.getId() + ",\"cantidad\":10}]}").andExpect(status().isCreated());
        assertThat(cuantasAlertas(v.getId())).isEqualTo(1);
        ajuste(v.getId(), -9, "Vuelve a bajar").andExpect(status().isCreated());
        assertThat(cuantasAlertas(v.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-2-42 llegar justo al mínimo ya cruza el umbral y usa el mínimo por defecto de la tienda si el SKU no tiene propio")
    void stockBajoEnElMinimoYConElPorDefecto() throws Exception {
        Variante sinPropio = nueva("INV08", "M", "negro", 6, 0);
        Variante conPropio = nueva("INV09", "M", "negro", 20, 0);
        jdbc.update("UPDATE stock SET minimo = 10 WHERE variante_id = ?", conPropio.getId());
        sincronizar();

        ajuste(sinPropio.getId(), -1, "Llega a 5").andExpect(status().isCreated());
        assertThat(cuantasAlertas(sinPropio.getId())).isEqualTo(1);
        ajuste(conPropio.getId(), -9, "Queda en 11").andExpect(status().isCreated());
        assertThat(cuantasAlertas(conPropio.getId())).isZero();
        ajuste(conPropio.getId(), -1, "Queda en 10").andExpect(status().isCreated());
        assertThat(cuantasAlertas(conPropio.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------ CA-2-43

    @Test
    @DisplayName("CA-2-43 cambia el mínimo de un SKU, lo vuelve al de la tienda con null y lo usa desde ese momento")
    void minimoPorSku() throws Exception {
        Variante v = nueva("INV10", "M", "negro", 8, 0);
        sincronizar();

        mvc.perform(patch(BASE + "/" + v.getId() + "/minimo").with(operador()).contentType(APPLICATION_JSON).content("{\"minimo\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minimo").value(10)).andExpect(jsonPath("$.data.minimoPropio").value(true))
                .andExpect(jsonPath("$.data.estado").value("BAJO"));
        mvc.perform(patch(BASE + "/" + v.getId() + "/minimo").with(operador()).contentType(APPLICATION_JSON).content("{\"minimo\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minimo").value(5)).andExpect(jsonPath("$.data.minimoPropio").value(false))
                .andExpect(jsonPath("$.data.estado").value("OK"));
        mvc.perform(patch(BASE + "/" + v.getId() + "/minimo").with(admin()).contentType(APPLICATION_JSON).content("{\"minimo\":1000}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(BASE + "/" + v.getId() + "/minimo").with(admin()).contentType(APPLICATION_JSON).content("{\"minimo\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(BASE + "/999999999/minimo").with(admin()).contentType(APPLICATION_JSON).content("{\"minimo\":3}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-2-43 el ADMIN cambia el mínimo por defecto y el margen de reserva de la tienda, con sus rangos, y se usan desde ese momento")
    void configuracionDeLaTienda() throws Exception {
        Variante v = nueva("INV11", "M", "negro", 9, 0);
        sincronizar();

        mvc.perform(get(BASE + "/configuracion").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockMinimoPorDefecto").value(5))
                .andExpect(jsonPath("$.data.margenReservaMinutos").value(5));
        mvc.perform(put(BASE + "/configuracion").with(admin()).contentType(APPLICATION_JSON)
                        .content("{\"stockMinimoPorDefecto\":8,\"margenReservaMinutos\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockMinimoPorDefecto").value(8))
                .andExpect(jsonPath("$.data.margenReservaMinutos").value(10));
        sincronizar();
        mvc.perform(get(BASE + "/configuracion").with(admin())).andExpect(jsonPath("$.data.margenReservaMinutos").value(10));

        // el nuevo mínimo por defecto (8) se usa en el cruce de umbral
        ajuste(v.getId(), -2, "Baja a 7").andExpect(status().isCreated());
        assertThat(cuantasAlertas(v.getId())).isEqualTo(1);
        em.flush();
        assertThat(jdbc.queryForObject("SELECT payload->>'minimo' FROM outbox_evento WHERE tipo = 'StockBajo' AND agregado_id = ?",
                String.class, String.valueOf(v.getId()))).isEqualTo("8");

        for (String invalido : List.of("{\"stockMinimoPorDefecto\":1000,\"margenReservaMinutos\":5}",
                "{\"stockMinimoPorDefecto\":-1,\"margenReservaMinutos\":5}", "{\"stockMinimoPorDefecto\":5,\"margenReservaMinutos\":31}",
                "{\"stockMinimoPorDefecto\":5,\"margenReservaMinutos\":-1}", "{\"stockMinimoPorDefecto\":5}")) {
            mvc.perform(put(BASE + "/configuracion").with(admin()).contentType(APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(BASE + "/configuracion").with(operador())).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/configuracion").with(operador()).contentType(APPLICATION_JSON)
                        .content("{\"stockMinimoPorDefecto\":1,\"margenReservaMinutos\":1}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ CA-2-44

    @Test
    @DisplayName("CA-2-44 filtra por solo stock bajo, solo agotados, texto y producto, con el estado de cada SKU")
    void filtros() throws Exception {
        Categoria c = categoria("Filtros de inventario", null);
        Producto p = producto("INV12", "Producto Filtros", "50.00", EstadoProducto.ACTIVO, c);
        Variante agotado = variante(p, "XS", "negro", 4, 4);
        Variante bajo = variante(p, "S", "negro", 3, 0);
        Variante normal = variante(p, "M", "negro", 20, 0);
        Variante conPropio = variante(p, "L", "negro", 5, 0);
        jdbc.update("UPDATE stock SET minimo = 2 WHERE variante_id = ?", conPropio.getId());
        Producto otro = producto("INV13", "Otro Producto", "50.00", EstadoProducto.BORRADOR, c);
        variante(otro, "M", "rojo", 1, 0);
        sincronizar();
        String filtroProducto = p.getId().toString();

        mvc.perform(get(BASE).with(operador()).param("productoId", filtroProducto).param("soloStockBajo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[*].varianteId", containsInAnyOrder(agotado.getId().intValue(), bajo.getId().intValue())));
        mvc.perform(get(BASE).with(operador()).param("productoId", filtroProducto).param("soloAgotados", "true"))
                .andExpect(jsonPath("$.data.contenido[*].varianteId", contains(agotado.getId().intValue())))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("AGOTADO"));
        mvc.perform(get(BASE).with(operador()).param("productoId", filtroProducto).param("sort", "disponible,desc"))
                .andExpect(jsonPath("$.data.tamano").value(20))
                .andExpect(jsonPath("$.data.contenido", hasSize(4)))
                .andExpect(jsonPath("$.data.contenido[0].varianteId").value(normal.getId().intValue()))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("OK"))
                .andExpect(jsonPath("$.data.contenido[?(@.varianteId==" + bajo.getId() + ")].estado").value("BAJO"))
                .andExpect(jsonPath("$.data.contenido[?(@.varianteId==" + conPropio.getId() + ")].estado").value("OK"))
                .andExpect(jsonPath("$.data.contenido[?(@.varianteId==" + conPropio.getId() + ")].minimoPropio").value(true))
                .andExpect(jsonPath("$.data.contenido[0].producto").value("Producto Filtros"))
                .andExpect(jsonPath("$.data.contenido[0].talla").value("M"))
                .andExpect(jsonPath("$.data.contenido[0].color").value("negro"));
        mvc.perform(get(BASE).with(operador()).param("q", "inv13"))
                .andExpect(jsonPath("$.data.contenido", hasSize(1)));
        mvc.perform(get(BASE).with(operador()).param("q", "producto filtros").param("size", "2"))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElementos").value(4));
        mvc.perform(get(BASE).with(operador()).param("sort", "inventado,asc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-39 el inventario exige sesión de staff: sin sesión 401 y un cliente 403")
    void permisos() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).with(com.fitstore.catalog.pruebas.Sesiones.cliente())).andExpect(status().isForbidden());
    }
}
