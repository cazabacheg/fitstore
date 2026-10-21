package com.fitstore.payment.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Página de pago del cliente: {@code GET /api/v1/pagos/{codigo}} (HU-5-02, CA-5-03 a CA-5-08, CA-5-24). */
class PagoControllerTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-03 muestra código, productos, total, vencimiento y los medios con credenciales más el pago de prueba")
    void muestraLaPagina() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codigoPedido").value(i.getCodigoPedido()))
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.monto").value(119.80))
                .andExpect(jsonPath("$.data.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.data.items[0].total").value(119.80))
                .andExpect(jsonPath("$.data.expiraEn").exists())
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')].clavePublica")
                        .value(hasItem("TEST-clave-publica-de-prueba")))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')].disponible").value(hasItem(true)))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='SIMULADO')].medio").value(hasItem("TARJETA")))
                .andExpect(jsonPath("$.data.simuladoHabilitado").value(true))
                .andExpect(jsonPath("$.data.procesando").value(false))
                .andExpect(jsonPath("$.data.boletaDisponible").value(false));
    }

    @Test
    @DisplayName("CA-5-03 un medio no habilitado en la tienda no se ofrece, aunque el proveedor tenga credenciales")
    void medioNoHabilitadoEnLaTienda() throws Exception {
        org.mockito.Mockito.when(tienda.metodosPago()).thenReturn(java.util.Optional.of(java.util.List.of("CULQI")));
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')]").isEmpty())
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='SIMULADO')]").isNotEmpty());
    }

    @Test
    @DisplayName("CA-5-03 si settings no responde se ofrecen los proveedores que tienen credenciales")
    void settingsCaido() throws Exception {
        org.mockito.Mockito.when(tienda.metodosPago()).thenReturn(java.util.Optional.empty());
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')]").isNotEmpty());
    }

    @Test
    @DisplayName("CA-5-04 sin intención todavía responde 404 PREPARANDO_PAGO para que la página reintente")
    void preparandoPago() throws Exception {
        mvc.perform(get("/api/v1/pagos/FS-999999").with(Sesiones.cliente(clienta)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PREPARANDO_PAGO"))
                .andExpect(jsonPath("$.mensaje").value("Preparando tu pago…"));
    }

    @Test
    @DisplayName("CA-5-06 un pedido ya pagado devuelve su estado PAGADA (la página muestra \"Ya pagaste este pedido\")")
    void yaPagado() throws Exception {
        IntencionPago i = intencion(clienta);
        i.setEstado(EstadoIntencion.PAGADA);
        intenciones.saveAndFlush(i);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PAGADA"));
    }

    @Test
    @DisplayName("CA-5-06 un pedido vencido o cancelado devuelve su vencimiento y su estado ANULADA")
    void vencidoOCancelado() throws Exception {
        IntencionPago vencida = intencion(clienta, "119.80", Instant.now().minus(5, ChronoUnit.MINUTES));
        IntencionPago anulada = intencion(clienta);
        anulada.setEstado(EstadoIntencion.ANULADA);
        intenciones.saveAndFlush(anulada);

        mvc.perform(get("/api/v1/pagos/" + vencida.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"));
        mvc.perform(get("/api/v1/pagos/" + anulada.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(jsonPath("$.data.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("CA-5-07 el pago de un pedido ajeno responde 404")
    void pedidoAjeno() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("CA-5-07 sin sesión responde 401 y con un rol de staff responde 403")
    void soloClientes() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.admin()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CA-5-24 con PAGOS_SIMULADO_HABILITADO=true aparece \"Pago de prueba\" y la página lo informa")
    void simuladoHabilitado() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(jsonPath("$.data.simuladoHabilitado").value(true))
                .andExpect(jsonPath("$.data.medios", not(hasSize(0))))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='SIMULADO')]").isNotEmpty());
    }
}
