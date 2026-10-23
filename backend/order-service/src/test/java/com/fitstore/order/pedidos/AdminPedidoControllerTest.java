package com.fitstore.order.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.Sesiones;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Detalle, avance de estados y acciones del operador en el backoffice (HU-4-09), con permisos y control de versión. */
class AdminPedidoControllerTest extends ApiBase {

    private final UUID cliente = UUID.randomUUID();

    private ResultActions avanzar(Pedido p, String nuevoEstado, long version, String extra) throws Exception {
        return mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/estado").with(Sesiones.operador())
                .contentType(APPLICATION_JSON)
                .content("{\"nuevoEstado\":\"" + nuevoEstado + "\",\"version\":" + version + extra + "}"));
    }

    private long version(Pedido p) {
        return jdbc.queryForObject("SELECT version FROM pedido WHERE id = ?", Long.class, p.getId());
    }

    // ---------- detalle y permisos ----------

    @Test
    @DisplayName("CA-4-43 el detalle trae cliente, destinatario, entrega, productos con SKU, montos, pago, boleta, historial y las transiciones permitidas")
    void detalleCompleto() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        p.setPagoProveedor("MERCADO_PAGO");
        p.setPagoMedio("TARJETA");
        p.setPagadoEn(Instant.now());
        p.setBoletaUrl("/api/v1/pagos/" + p.getCodigo() + "/boleta");
        p.agregarHistorial(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, TipoActor.SISTEMA, null, "Pago confirmado");
        pedidos.saveAndFlush(p);

        mvc.perform(get("/api/v1/admin/pedidos/" + p.getId()).with(Sesiones.operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.data.clienteId").value(cliente.toString()))
                .andExpect(jsonPath("$.data.codigo").value(p.getCodigo()))
                .andExpect(jsonPath("$.data.version").value(version(p)))
                .andExpect(jsonPath("$.data.destinatario.dni").value("12345678"))
                .andExpect(jsonPath("$.data.direccion.direccion").value("Jr. Gamarra 123"))
                .andExpect(jsonPath("$.data.envio.modalidad").value("DELIVERY_LOCAL"))
                .andExpect(jsonPath("$.data.items[0].sku").value("POL-NEG-M"))
                .andExpect(jsonPath("$.data.total").value(128.0))
                .andExpect(jsonPath("$.data.pago.proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.data.boletaUrl").exists())
                .andExpect(jsonPath("$.data.historial", hasSize(2)))
                .andExpect(jsonPath("$.data.transicionesPermitidas", hasSize(1)))
                .andExpect(jsonPath("$.data.transicionesPermitidas[0].estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.data.transicionesPermitidas[0].etiqueta").value("Empezar a preparar"))
                .andExpect(jsonPath("$.data.puedeCancelarStaff").value(true))
                .andExpect(jsonPath("$.data.reembolsoPostventa.permitido").value(false))
                .andExpect(jsonPath("$.data.reembolsos", hasSize(0)));
    }

    @Test
    @DisplayName("CA-4-43 un pedido final (entregado) no ofrece ninguna acción")
    void pedidoFinalSinAcciones() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.ENTREGADO, Modalidad.AGENCIA);

        mvc.perform(get("/api/v1/admin/pedidos/" + p.getId()).with(Sesiones.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transicionesPermitidas", hasSize(0)))
                .andExpect(jsonPath("$.data.puedeCancelarStaff").value(false));
    }

    @Test
    @DisplayName("CA-4-43 solo el staff ve el backoffice: sin sesión 401, cliente 403, pedido inexistente 404")
    void permisos() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        mvc.perform(get("/api/v1/admin/pedidos/" + p.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/pedidos/" + p.getId()).with(Sesiones.cliente(cliente)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/pedidos/" + UUID.randomUUID()).with(Sesiones.operador()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/pedidos").with(Sesiones.cliente(cliente))).andExpect(status().isForbidden());
        avanzarSinSesion(p);
    }

    private void avanzarSinSesion(Pedido p) throws Exception {
        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/estado").contentType(APPLICATION_JSON)
                .content("{\"nuevoEstado\":\"EN_PREPARACION\",\"version\":0}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/estado").with(Sesiones.cliente(cliente))
                .contentType(APPLICATION_JSON).content("{\"nuevoEstado\":\"EN_PREPARACION\",\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    // ---------- HU-4-09: avanzar ----------

    @Test
    @DisplayName("CA-4-44 un pedido PAGADO de delivery pasa a EN_PREPARACION y a LISTO_DESPACHO, con mi usuario en el historial, y publica PedidoListoDespacho")
    void preparaYDespacha() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        avanzar(p, "EN_PREPARACION", version(p), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.data.transicionesPermitidas[0].estado").value("LISTO_DESPACHO"));
        sincronizar();
        avanzar(p, "LISTO_DESPACHO", version(p), ",\"comentario\":\"2 bultos\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("LISTO_DESPACHO"))
                .andExpect(jsonPath("$.data.transicionesPermitidas", hasSize(0)))
                .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='EN_PREPARACION')].actorNombre")
                        .value(contains("Omar Operador")))
                .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='LISTO_DESPACHO')].actorTipo")
                        .value(contains("STAFF")))
                .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='LISTO_DESPACHO')].comentario")
                        .value(contains("2 bultos")));

        assertThat(eventos("PedidoListoDespacho")).hasSize(1);
        JsonNode evento = datos(eventos("PedidoListoDespacho").get(0));
        assertThat(evento.get("codigo").asString()).isEqualTo(p.getCodigo());
        assertThat(evento.get("modalidad").asString()).isEqualTo("DELIVERY_LOCAL");
        assertThat(evento.get("destinatario").get("nombres").asString()).isEqualTo("Ana");
        assertThat(evento.get("direccion").get("distrito").asString()).isEqualTo("Lima");
        assertThat(evento.get("resumenItems").asString()).contains("Polo deportivo");
        assertThat(evento.get("bultos").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM pedido_historial WHERE pedido_id = ? "
                + "AND estado_nuevo = 'EN_PREPARACION'", String.class, p.getId())).isEqualTo(Sesiones.OPERADOR_ID);
    }

    @Test
    @DisplayName("CA-4-46 un recojo en EN_PREPARACION pasa a LISTO_RECOJO y publica PedidoListoRecojo")
    void listoParaRecoger() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.EN_PREPARACION, Modalidad.RECOJO);

        avanzar(p, "LISTO_RECOJO", version(p), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("LISTO_RECOJO"))
                .andExpect(jsonPath("$.data.transicionesPermitidas[*].estado").value(contains("ENTREGADO", "DEVUELTO")));

        assertThat(eventos("PedidoListoRecojo")).hasSize(1);
        assertThat(datos(eventos("PedidoListoRecojo").get(0)).get("destinatario").get("email").asString())
                .isEqualTo("ana@correo.pe");
    }

    @Test
    @DisplayName("CA-4-46 marcar \"Recogido\" exige confirmar que se verificó el DNI; pasa a ENTREGADO y publica PedidoRecogido")
    void recogido() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.LISTO_RECOJO, Modalidad.RECOJO);

        avanzar(p, "ENTREGADO", version(p), "").andExpect(status().isBadRequest());
        avanzar(p, "ENTREGADO", version(p), ",\"dniVerificado\":false").andExpect(status().isBadRequest());
        assertThat(eventos("PedidoRecogido")).isEmpty();

        avanzar(p, "ENTREGADO", version(p), ",\"dniVerificado\":true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ENTREGADO"));
        assertThat(eventos("PedidoRecogido")).hasSize(1);
    }

    @Test
    @DisplayName("CA-4-60 un pedido LISTO_RECOJO que nadie recogió pasa a DEVUELTO con el motivo (obligatorio) y mi usuario")
    void noRecogido() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.LISTO_RECOJO, Modalidad.RECOJO);

        avanzar(p, "DEVUELTO", version(p), "").andExpect(status().isBadRequest());
        avanzar(p, "DEVUELTO", version(p), ",\"comentario\":\"  \"").andExpect(status().isBadRequest());

        avanzar(p, "DEVUELTO", version(p), ",\"comentario\":\"No lo recogió en 15 días\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.data.transicionesPermitidas", hasSize(0)))
                .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='DEVUELTO')].comentario")
                        .value(contains("No lo recogió en 15 días")))
                .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='DEVUELTO')].actorNombre")
                        .value(contains("Omar Operador")));
    }

    @Test
    @DisplayName("CA-4-47 una transición no permitida (PAGADO → ENVIADO, LISTO_RECOJO con agencia) responde 409 con el estado actual y nada cambia")
    void transicionNoPermitida() throws Exception {
        Pedido pagado = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        Pedido agencia = pedido(cliente, EstadoPedido.EN_PREPARACION, Modalidad.AGENCIA);

        avanzar(pagado, "ENVIADO", version(pagado), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"))
                .andExpect(jsonPath("$.detalles.estadoActual").value("PAGADO"));
        avanzar(agencia, "LISTO_RECOJO", version(agencia), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, pagado.getId()))
                .isEqualTo("PAGADO");
        assertThat(eventos("PedidoListoRecojo")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-48 si otro operador cambió el pedido, avanzar con la versión vieja responde 409 \"El pedido cambió; recarga la página\"")
    void versionVieja() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        long vista = version(p);

        avanzar(p, "EN_PREPARACION", vista, "").andExpect(status().isOk());
        sincronizar();

        avanzar(p, "LISTO_DESPACHO", vista, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"))
                .andExpect(jsonPath("$.mensaje").value("El pedido cambió; recarga la página"));
        assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, p.getId()))
                .isEqualTo("EN_PREPARACION");
    }

    @Test
    @DisplayName("CA-4-48 sin la versión o con un estado desconocido la petición es inválida: 400")
    void peticionInvalida() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/estado").with(Sesiones.operador())
                .contentType(APPLICATION_JSON).content("{\"nuevoEstado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/estado").with(Sesiones.operador())
                .contentType(APPLICATION_JSON).content("{\"nuevoEstado\":\"VOLANDO\",\"version\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- HU-4-08: lista, filtros y contadores ----------

    @Test
    @DisplayName("CA-4-40 la tabla trae código, fecha, cliente, modalidad, estado y total, con lo que hay que preparar primero y un contador por estado")
    void listaConContadores() throws Exception {
        pedido(cliente, EstadoPedido.ENTREGADO, Modalidad.DELIVERY_LOCAL);
        Pedido pagado = pedido(cliente, EstadoPedido.PAGADO, Modalidad.AGENCIA);
        pedido(cliente, EstadoPedido.PENDIENTE_PAGO, Modalidad.RECOJO);
        pedido(cliente, EstadoPedido.PAGADO, Modalidad.RECOJO);

        mvc.perform(get("/api/v1/admin/pedidos").with(Sesiones.operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(4)))
                .andExpect(jsonPath("$.data.totalElementos").value(4))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("PAGADO"))
                .andExpect(jsonPath("$.data.contenido[0].id").value(pagado.getId().toString()))
                .andExpect(jsonPath("$.data.contenido[0].codigo").value(pagado.getCodigo()))
                .andExpect(jsonPath("$.data.contenido[0].clienteNombre").value("Ana Quispe"))
                .andExpect(jsonPath("$.data.contenido[0].modalidad").value("AGENCIA"))
                .andExpect(jsonPath("$.data.contenido[0].total").value(128.0))
                .andExpect(jsonPath("$.data.contenido[2].estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.data.contadores.PAGADO").value(2))
                .andExpect(jsonPath("$.data.contadores.PENDIENTE_PAGO").value(1))
                .andExpect(jsonPath("$.data.contadores.ENTREGADO").value(1))
                .andExpect(jsonPath("$.data.contadores.CANCELADO").value(0));
    }

    @Test
    @DisplayName("CA-4-41 los filtros de estado, modalidad, fechas, código y cliente se combinan y los contadores ignoran el estado")
    void filtrosCombinados() throws Exception {
        Pedido a = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        pedido(cliente, EstadoPedido.PAGADO, Modalidad.RECOJO);
        pedido(cliente, EstadoPedido.ENTREGADO, Modalidad.DELIVERY_LOCAL);
        String hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();

        mvc.perform(get("/api/v1/admin/pedidos?estado=PAGADO&modalidad=DELIVERY_LOCAL&cliente=quispe&desde=" + hoy
                + "&hasta=" + hoy + "&codigo=" + a.getCodigo()).with(Sesiones.operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(1)))
                .andExpect(jsonPath("$.data.contenido[0].codigo").value(a.getCodigo()))
                .andExpect(jsonPath("$.data.contadores.PAGADO").value(1))
                .andExpect(jsonPath("$.data.contadores.ENTREGADO").value(0));
        mvc.perform(get("/api/v1/admin/pedidos?estado=EXPIRADO&modalidad=DELIVERY_LOCAL").with(Sesiones.admin()))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)))
                .andExpect(jsonPath("$.data.contadores.PAGADO").value(1))
                .andExpect(jsonPath("$.data.contadores.ENTREGADO").value(1));
        mvc.perform(get("/api/v1/admin/pedidos?desde=2020-01-01&hasta=2020-01-31").with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.contenido", hasSize(0)));
        mvc.perform(get("/api/v1/admin/pedidos?codigo=abc").with(Sesiones.operador())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/pedidos?estado=VOLANDO").with(Sesiones.operador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-4-42 con clienteId solo se ven los pedidos de ese cliente; un orden explícito se respeta")
    void porClienteYOrden() throws Exception {
        UUID otro = UUID.randomUUID();
        pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        Pedido barato = pedido(cliente, EstadoPedido.PAGADO, Modalidad.RECOJO);
        jdbc.update("UPDATE pedido SET total = 10.00 WHERE id = ?", barato.getId());
        pedido(otro, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        sincronizar();

        mvc.perform(get("/api/v1/admin/pedidos?clienteId=" + cliente + "&sort=total,asc").with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)))
                .andExpect(jsonPath("$.data.contenido[0].total").value(10.0));
        mvc.perform(get("/api/v1/admin/pedidos?clienteId=" + cliente + "&sort=total,desc&sort=noexiste").with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.contenido[0].total").value(128.0));
    }

    // ---------- extensión de alcance: responden 501 con la misma firma del contrato ----------

    @Test
    @DisplayName("extensión de alcance: el resumen de ventas y el reembolso postventa responden 501 EXTENSION_DE_ALCANCE")
    void extensionesDeAlcance() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.ENTREGADO, Modalidad.DELIVERY_LOCAL);

        // POST /carrito/fusionar ya no es extensión de alcance (SPEC-4 v12, HU-4-02): lo cubre FusionCarritoTest
        mvc.perform(get("/api/v1/admin/pedidos/resumen").with(Sesiones.admin()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
        mvc.perform(post("/api/v1/admin/pedidos/" + p.getId() + "/reembolsos").with(Sesiones.admin())
                .contentType(APPLICATION_JSON)
                .content("{\"items\":[{\"varianteId\":10,\"cantidad\":1}],\"monto\":10.5,\"motivo\":\"DEFECTUOSO\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }
}
