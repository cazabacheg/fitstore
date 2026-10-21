package com.fitstore.order.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.Sesiones;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Cancelación por el cliente (HU-4-07) y por el staff (HU-4-10): quién, cuándo y qué evento publica. */
class CancelacionTest extends ApiBase {

    private final UUID cliente = UUID.randomUUID();

    private ResultActions cancelarComoCliente(Pedido p) throws Exception {
        return mvc.perform(patch("/api/v1/pedidos/" + p.getCodigo() + "/cancelar").with(Sesiones.cliente(cliente)));
    }

    private ResultActions cancelarComoStaff(Pedido p, long version, String motivo) throws Exception {
        return mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/cancelar").with(Sesiones.operador())
                .contentType(APPLICATION_JSON)
                .content("{\"motivo\":" + (motivo == null ? "null" : json.writeValueAsString(motivo))
                        + ",\"version\":" + version + "}"));
    }

    // ---------- HU-4-07: el cliente ----------

    @Test
    @DisplayName("CA-4-38 el cliente cancela un pedido PENDIENTE_PAGO: queda CANCELADO y publica PedidoCancelado sin pago")
    void clienteCancelaPendiente() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);

        cancelarComoCliente(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.data.puedeCancelar").value(false))
                .andExpect(jsonPath("$.data.historial[1].estadoNuevo").value("CANCELADO"))
                .andExpect(jsonPath("$.data.historial[1].actorTipo").value("CLIENTE"));

        List<com.fitstore.common.event.OutboxEvento> cancelados = eventos("PedidoCancelado");
        assertThat(cancelados).hasSize(1);
        JsonNode datos = datos(cancelados.get(0));
        assertThat(datos.get("codigo").asString()).isEqualTo(p.getCodigo());
        assertThat(datos.get("fuePagado").asBoolean()).isFalse();
        assertThat(datos.get("actor").asString()).isEqualTo("CLIENTE");
        assertThat(datos.get("items")).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, p.getId()))
                .isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("CA-4-38 cancelar un pedido PAGADO publica PedidoCancelado con fuePagado para que payment reembolse")
    void clienteCancelaPagado() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        cancelarComoCliente(p).andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("CANCELADO"));

        assertThat(datos(eventos("PedidoCancelado").get(0)).get("fuePagado").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("CA-4-39 desde EN_PREPARACION el cliente no puede cancelar: 409 \"El pedido ya está en preparación\"")
    void clienteNoCancelaEnPreparacion() throws Exception {
        for (EstadoPedido estado : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_DESPACHO, EstadoPedido.ENVIADO,
                EstadoPedido.ENTREGADO)) {
            Pedido p = pedido(cliente, estado, Modalidad.DELIVERY_LOCAL);

            cancelarComoCliente(p)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_CANCELABLE"))
                    .andExpect(jsonPath("$.mensaje").value("El pedido ya está en preparación"));
            assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, p.getId()))
                    .isEqualTo(estado.name());
        }
        assertThat(eventos("PedidoCancelado")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-39 cancelar el pedido de otro cliente responde 404 y no lo cambia")
    void clienteNoCancelaAjeno() throws Exception {
        Pedido ajeno = pedido(UUID.randomUUID(), EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);

        cancelarComoCliente(ajeno).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, ajeno.getId()))
                .isEqualTo("PENDIENTE_PAGO");
    }

    // ---------- HU-4-10: el staff ----------

    @Test
    @DisplayName("CA-4-49 el staff cancela en PENDIENTE_PAGO, PAGADO o EN_PREPARACION con motivo; queda su usuario y se publica PedidoCancelado")
    void staffCancela() throws Exception {
        for (EstadoPedido estado : List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION)) {
            Pedido p = pedido(cliente, estado, Modalidad.DELIVERY_LOCAL);

            cancelarComoStaff(p, p.getVersion(), "Error de stock en el almacén")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='CANCELADO')].comentario")
                            .value(contains("Error de stock en el almacén")))
                    .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='CANCELADO')].actorTipo")
                            .value(contains("STAFF")))
                    .andExpect(jsonPath("$.data.historial[?(@.estadoNuevo=='CANCELADO')].actorNombre")
                            .value(contains("Omar Operador")))
                    .andExpect(jsonPath("$.data.transicionesPermitidas", hasSize(0)));

            JsonNode evento = datos(eventos("PedidoCancelado").stream()
                    .filter(e -> e.getAgregadoId().equals(p.getId().toString())).findFirst().orElseThrow());
            assertThat(evento.get("actor").asString()).isEqualTo("STAFF");
            assertThat(evento.get("motivo").asString()).isEqualTo("Error de stock en el almacén");
            assertThat(evento.get("fuePagado").asBoolean()).isEqualTo(estado != EstadoPedido.PENDIENTE_PAGO);
        }
    }

    @Test
    @DisplayName("CA-4-49 el motivo es obligatorio y admite hasta 300 caracteres")
    void motivoObligatorio() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        cancelarComoStaff(p, p.getVersion(), null).andExpect(status().isBadRequest());
        cancelarComoStaff(p, p.getVersion(), "  ").andExpect(status().isBadRequest());
        cancelarComoStaff(p, p.getVersion(), "x".repeat(301)).andExpect(status().isBadRequest());
        cancelarComoStaff(p, p.getVersion(), "x".repeat(300)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-4-50 desde LISTO_DESPACHO ya no se cancela: 409 y nada cambia")
    void staffNoCancelaDespuesDelDespacho() throws Exception {
        for (EstadoPedido estado : List.of(EstadoPedido.LISTO_DESPACHO, EstadoPedido.LISTO_RECOJO, EstadoPedido.ENVIADO,
                EstadoPedido.ENTREGADO, EstadoPedido.DEVUELTO)) {
            Pedido p = pedido(cliente, estado, Modalidad.DELIVERY_LOCAL);

            cancelarComoStaff(p, p.getVersion(), "Quiero cancelarlo")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_CANCELABLE"));
            assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, p.getId()))
                    .isEqualTo(estado.name());
        }
        assertThat(eventos("PedidoCancelado")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-50 el detalle no ofrece cancelar desde LISTO_DESPACHO (botón oculto)")
    void botonOculto() throws Exception {
        Pedido listo = pedido(cliente, EstadoPedido.LISTO_DESPACHO, Modalidad.DELIVERY_LOCAL);
        Pedido pagado = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/admin/pedidos/" + listo.getId()).with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.puedeCancelarStaff").value(false));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/admin/pedidos/" + pagado.getId()).with(Sesiones.operador()))
                .andExpect(jsonPath("$.data.puedeCancelarStaff").value(true));
    }

    @Test
    @DisplayName("CA-4-49 solo el staff cancela por el backoffice: un cliente recibe 403 y sin sesión 401")
    void permisosDeCancelacion() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        String cuerpo = "{\"motivo\":\"Duplicado\",\"version\":" + p.getVersion() + "}";

        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/cancelar").with(Sesiones.cliente(cliente))
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/pedidos/" + p.getId() + "/cancelar")
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/pedidos/" + UUID.randomUUID() + "/cancelar").with(Sesiones.admin())
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-4-49 si otro operador cambió el pedido, cancelar con la versión vieja responde 409 CONFLICTO_VERSION")
    void cancelarConVersionVieja() throws Exception {
        Pedido p = pedido(cliente, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);
        long vieja = p.getVersion();
        jdbc.update("UPDATE pedido SET version = version + 1 WHERE id = ?", p.getId());
        sincronizar();

        cancelarComoStaff(p, vieja, "Duplicado")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
    }
}
