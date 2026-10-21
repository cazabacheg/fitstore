package com.fitstore.order.pedidos;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.Sesiones;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Mis pedidos y su seguimiento (HU-4-06): lista paginada, detalle con línea de tiempo y pedidos ajenos. */
class PedidoControllerTest extends ApiBase {

    private final UUID cliente = UUID.randomUUID();

    @Test
    @DisplayName("CA-4-33 mis pedidos salen del más reciente al más antiguo, 10 por página, con código, fecha, estado, total y miniatura")
    void listaPaginada() throws Exception {
        for (int i = 0; i < 12; i++) {
            Pedido p = pedido(cliente, EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
            jdbc.update("UPDATE pedido SET creado_en = ? WHERE id = ?",
                    java.sql.Timestamp.from(Instant.now().minus(12 - i, ChronoUnit.HOURS)), p.getId());
        }
        pedido(UUID.randomUUID(), EstadoPedido.PAGADO, Modalidad.RECOJO); // de otro cliente
        sincronizar();

        mvc.perform(get("/api/v1/pedidos").with(Sesiones.cliente(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(10)))
                .andExpect(jsonPath("$.data.totalElementos").value(12))
                .andExpect(jsonPath("$.data.totalPaginas").value(2))
                .andExpect(jsonPath("$.data.contenido[0].estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.data.contenido[0].total").value(128.0))
                .andExpect(jsonPath("$.data.contenido[0].cantidadProductos").value(3))
                .andExpect(jsonPath("$.data.contenido[0].miniaturaUrl").value("http://media/polo.jpg"))
                .andExpect(jsonPath("$.data.contenido[0].expiraEn").exists())
                .andExpect(jsonPath("$.data.contenido[0].creadoEn").exists());

        // El más reciente primero: el último de la página 0 es más antiguo que el primero
        var pagina0 = cuerpo(mvc.perform(get("/api/v1/pedidos").with(Sesiones.cliente(cliente))))
                .get("data").get("contenido");
        org.assertj.core.api.Assertions.assertThat(pagina0.get(0).get("creadoEn").asString())
                .isGreaterThan(pagina0.get(9).get("creadoEn").asString());
        mvc.perform(get("/api/v1/pedidos?page=1").with(Sesiones.cliente(cliente)))
                .andExpect(jsonPath("$.data.contenido", hasSize(2)));
    }

    @Test
    @DisplayName("CA-4-33 un cliente sin pedidos recibe una página vacía")
    void sinPedidos() throws Exception {
        mvc.perform(get("/api/v1/pedidos").with(Sesiones.cliente(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElementos").value(0));
    }

    @Test
    @DisplayName("CA-4-34 el detalle trae la línea de tiempo con fecha de cada paso, los productos, la entrega y los montos")
    void detalleConLineaDeTiempo() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.ENVIADO, Modalidad.AGENCIA);
        p.getEnvio().setAgenciaNombre("Shalom");
        p.getEnvio().setCodigoSeguimiento("SH-123");
        p.agregarHistorial(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, TipoActor.SISTEMA, null, "Pago confirmado");
        p.agregarHistorial(EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, TipoActor.STAFF, UUID.randomUUID(),
                "Omar Operador", null);
        p.agregarHistorial(EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_DESPACHO, TipoActor.STAFF, UUID.randomUUID(),
                "Omar Operador", null);
        p.agregarHistorial(EstadoPedido.LISTO_DESPACHO, EstadoPedido.ENVIADO, TipoActor.SISTEMA, null,
                "Enviado por Shalom, código SH-123");
        p.agregarHistorial(EstadoPedido.ENVIADO, EstadoPedido.ENVIADO, TipoActor.SISTEMA, null,
                "Intento fallido (1): Cliente ausente");
        pedidos.saveAndFlush(p);

        mvc.perform(get("/api/v1/pedidos/" + p.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(p.getCodigo()))
                .andExpect(jsonPath("$.data.estado").value("ENVIADO"))
                .andExpect(jsonPath("$.data.historial", hasSize(6)))
                .andExpect(jsonPath("$.data.historial[0].tipo").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$.data.historial[0].estadoNuevo").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.data.historial[0].fecha").exists())
                .andExpect(jsonPath("$.data.historial[4].comentario").value("Enviado por Shalom, código SH-123"))
                .andExpect(jsonPath("$.data.historial[5].tipo").value("INTENTO_FALLIDO"))
                .andExpect(jsonPath("$.data.historial[2].actorNombre", nullValue()))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].producto").value("Polo deportivo"))
                .andExpect(jsonPath("$.data.envio.agenciaNombre").value("Shalom"))
                .andExpect(jsonPath("$.data.envio.codigoSeguimiento").value("SH-123"))
                .andExpect(jsonPath("$.data.direccion.distrito").value("Lima"))
                .andExpect(jsonPath("$.data.destinatario.nombres").value("Ana"))
                .andExpect(jsonPath("$.data.subtotal").value(130.0))
                .andExpect(jsonPath("$.data.descuentoTotal").value(10.0))
                .andExpect(jsonPath("$.data.costoEnvio").value(8.0))
                .andExpect(jsonPath("$.data.total").value(128.0))
                .andExpect(jsonPath("$.data.estadoReembolso").value("NINGUNO"))
                .andExpect(jsonPath("$.data.puedeCancelar").value(false));
    }

    @Test
    @DisplayName("CA-4-34 un pedido devuelto termina en DEVUELTO con el motivo en su último paso")
    void pedidoDevuelto() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.DEVUELTO, Modalidad.RECOJO);
        p.agregarHistorial(EstadoPedido.LISTO_RECOJO, EstadoPedido.DEVUELTO, TipoActor.STAFF, UUID.randomUUID(),
                "No lo recogió en 15 días");
        pedidos.saveAndFlush(p);

        mvc.perform(get("/api/v1/pedidos/" + p.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.data.direccion", nullValue()))
                .andExpect(jsonPath("$.data.historial[1].estadoNuevo").value("DEVUELTO"))
                .andExpect(jsonPath("$.data.historial[1].comentario").value("No lo recogió en 15 días"));
    }

    @Test
    @DisplayName("CA-4-35 un pedido PENDIENTE_PAGO muestra cuándo vence y que se puede pagar")
    void pendienteDePago() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);

        mvc.perform(get("/api/v1/pedidos/" + p.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiraEn").exists())
                .andExpect(jsonPath("$.data.puedePagar").value(true))
                .andExpect(jsonPath("$.data.puedeCancelar").value(true));
    }

    @Test
    @DisplayName("CA-4-35 vencido el plazo ya no se ofrece pagar, y un pedido pagado tampoco")
    void sinOpcionDePagar() throws Exception {
        Pedido vencido = pedido(cliente, EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        jdbc.update("UPDATE pedido SET expira_en = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)), vencido.getId());
        Pedido pagado = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        sincronizar();

        mvc.perform(get("/api/v1/pedidos/" + vencido.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(jsonPath("$.data.puedePagar").value(false));
        mvc.perform(get("/api/v1/pedidos/" + pagado.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(jsonPath("$.data.puedePagar").value(false))
                .andExpect(jsonPath("$.data.expiraEn", nullValue()));
    }

    @Test
    @DisplayName("CA-4-36 con boleta el detalle trae el enlace de descarga; sin boleta, no")
    void conBoleta() throws Exception {
        Pedido sin = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        Pedido con = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        con.setBoletaUrl("/api/v1/pagos/" + con.getCodigo() + "/boleta");
        con.setPagoProveedor("MERCADO_PAGO");
        con.setPagoMedio("TARJETA");
        con.setPagadoEn(Instant.now());
        pedidos.saveAndFlush(con);

        mvc.perform(get("/api/v1/pedidos/" + con.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(jsonPath("$.data.boletaUrl").value("/api/v1/pagos/" + con.getCodigo() + "/boleta"))
                .andExpect(jsonPath("$.data.pago.proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.data.pago.medio").value("TARJETA"));
        mvc.perform(get("/api/v1/pedidos/" + sin.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(jsonPath("$.data.boletaUrl", nullValue()))
                .andExpect(jsonPath("$.data.pago", nullValue()));
    }

    @Test
    @DisplayName("CA-4-37 pedir el código de un pedido ajeno o inexistente responde 404, no 403")
    void pedidoAjeno() throws Exception {
        Pedido ajeno = pedido(UUID.randomUUID(), EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        mvc.perform(get("/api/v1/pedidos/" + ajeno.getCodigo()).with(Sesiones.cliente(cliente)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pedidos/FS-999999").with(Sesiones.cliente(cliente)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-4-37 sin sesión responde 401 y con la sesión de un operador 403")
    void soloClientes() throws Exception {
        mvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pedidos").with(Sesiones.operador())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/FS-000001").with(Sesiones.operador())).andExpect(status().isForbidden());
    }
}
