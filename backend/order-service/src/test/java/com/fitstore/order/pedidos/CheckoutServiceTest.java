package com.fitstore.order.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.EntornoApi;
import com.fitstore.order.pruebas.Sesiones;
import com.fitstore.order.pruebas.Simulados;
import com.fitstore.order.pruebas.Simulados.Linea;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Resumen y confirmación del checkout (HU-4-03 y HU-4-04) con catalog, auth, delivery y settings en WireMock.
 * Escenario base: 2 leggings a S/ 59.90 con 10 % y 1 top a S/ 39.90; envío de S/ 12.00 → total S/ 159.72.
 */
class CheckoutServiceTest extends ApiBase {

    private final UUID cliente = UUID.randomUUID();
    private static final List<String> TODAS = List.of("DELIVERY_LOCAL", "AGENCIA", "RECOJO");

    @BeforeEach
    void escenarioBase() {
        carritos.establecerLinea(CarritoId.cliente(cliente), 101, 2);
        carritos.establecerLinea(CarritoId.cliente(cliente), 230, 1);
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2).conDescuento("10"),
                Linea.ok(230, "Top Flex", "39.90", 1));
        Simulados.perfil(cliente.toString());
        Simulados.direccion(7, "150101");
        Simulados.ubigeo("150101", "Lima", "Lima", "Lima");
        Simulados.configuracion(TODAS, 30);
        Simulados.cotizarEnvio("12.00", 1, 2, null);
        Simulados.reservaCreada();
    }

    private ResultActions resumen(String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/checkout/resumen").with(Sesiones.cliente(cliente))
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions confirmar(String clave, String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/pedidos").with(Sesiones.cliente(cliente)).header("Idempotency-Key", clave)
                .contentType(APPLICATION_JSON).content(cuerpo));
    }

    private static String delivery(String total) {
        return "{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7,\"notas\":\"Tocar el timbre\",\"totalEsperado\":" + total + "}";
    }

    // ---------- HU-4-03: resumen ----------

    @Test
    @DisplayName("CA-4-13 el resumen de una entrega a domicilio trae los ítems, la dirección con sus nombres y el envío")
    void resumenDelivery() throws Exception {
        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].producto").value("Legging Pro"))
                .andExpect(jsonPath("$.data.items[0].descuentoMonto").value(11.98))
                .andExpect(jsonPath("$.data.subtotal").value(159.7))
                .andExpect(jsonPath("$.data.descuentoTotal").value(11.98))
                .andExpect(jsonPath("$.data.costoEnvio").value(12.0))
                .andExpect(jsonPath("$.data.total").value(159.72))
                .andExpect(jsonPath("$.data.envio.plazoDiasMin").value(1))
                .andExpect(jsonPath("$.data.envio.plazoDiasMax").value(2))
                .andExpect(jsonPath("$.data.direccion.distrito").value("Lima"))
                .andExpect(jsonPath("$.data.direccion.direccion").value("Jr. Gamarra 123"))
                .andExpect(jsonPath("$.data.expiracionMinutos").value(30));
    }

    @Test
    @DisplayName("CA-4-13 el recojo en tienda muestra la dirección y el horario de la tienda, sin costo")
    void resumenRecojo() throws Exception {
        resumen("{\"modalidad\":\"RECOJO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.costoEnvio").value(0))
                .andExpect(jsonPath("$.data.total").value(147.72))
                .andExpect(jsonPath("$.data.direccion").doesNotExist())
                .andExpect(jsonPath("$.data.envio.modalidad").value("RECOJO"))
                .andExpect(jsonPath("$.data.envio.tiendaDireccion").value(startsWith("Jr. Gamarra 500")));
        EntornoApi.SERVICIOS.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/v1/envios/cotizar")));
    }

    @Test
    @DisplayName("CA-4-13 una modalidad que la tienda no habilitó responde 400")
    void modalidadDeshabilitada() throws Exception {
        Simulados.configuracion(List.of("AGENCIA", "RECOJO"), 30);

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MODALIDAD_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("CA-4-14 el delivery local a una dirección fuera de Lima y Callao responde 400")
    void deliveryFueraDeLima() throws Exception {
        Simulados.direccion(8, "040101");

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":8}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_COBERTURA"));
        confirmar("k-fuera", "{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":8,\"totalEsperado\":159.72}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_COBERTURA"));
        assertThat(pedidos.count()).isZero();
    }

    @Test
    @DisplayName("CA-4-14 el Callao (07) sí cuenta como cobertura de delivery local")
    void deliveryEnElCallao() throws Exception {
        Simulados.direccion(9, "070101");
        Simulados.ubigeo("070101", "Callao", "Callao", "Callao");

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":9}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direccion.departamento").value("Callao"));
    }

    @Test
    @DisplayName("CA-4-15 la agencia es obligatoria y el resumen trae su costo, plazo y nombre")
    void agencia() throws Exception {
        Simulados.direccion(8, "040101");
        Simulados.ubigeo("040101", "Arequipa", "Arequipa", "Arequipa");
        Simulados.cotizarEnvio("18.00", 2, 3, "Shalom");

        resumen("{\"modalidad\":\"AGENCIA\",\"direccionId\":8}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("AGENCIA_REQUERIDA"));

        resumen("{\"modalidad\":\"AGENCIA\",\"direccionId\":8,\"agenciaId\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.envio.agenciaId").value(1))
                .andExpect(jsonPath("$.data.envio.agenciaNombre").value("Shalom"))
                .andExpect(jsonPath("$.data.envio.costo").value(18.0))
                .andExpect(jsonPath("$.data.envio.plazoDiasMax").value(3))
                .andExpect(jsonPath("$.data.total").value(165.72));
        EntornoApi.SERVICIOS.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/v1/envios/cotizar"))
                .withRequestBody(WireMock.matchingJsonPath("$[?(@.modalidad == 'AGENCIA' && @.ubigeo == '040101' && @.agenciaId == 1)]")));
    }

    @Test
    @DisplayName("CA-4-16 el resumen no reserva stock ni guarda nada")
    void resumenNoGuardaNada() throws Exception {
        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}").andExpect(status().isOk());

        assertThat(pedidos.count()).isZero();
        assertThat(eventos("PedidoCreado")).isEmpty();
        EntornoApi.SERVICIOS.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/v1/inventario/reservas")));
        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).hasSize(2);
    }

    @Test
    @DisplayName("CA-4-16 con el carrito vacío el resumen responde 400")
    void resumenConCarritoVacio() throws Exception {
        carritos.vaciar(CarritoId.cliente(cliente));

        resumen("{\"modalidad\":\"RECOJO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CARRITO_VACIO"));
    }

    @Test
    @DisplayName("CA-4-16 una línea que ya no está disponible bloquea el resumen con 409")
    void resumenConLineaNoDisponible() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2),
                Linea.ok(230, "Top Flex", "39.90", 1).conEstado("NO_DISPONIBLE", null));

        resumen("{\"modalidad\":\"RECOJO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("LINEAS_NO_DISPONIBLES"));
    }

    @Test
    @DisplayName("CA-4-18 si auth, delivery o settings no responden el resumen responde 503 y no calcula el envío")
    void dependenciaCaidaEnElResumen() throws Exception {
        Simulados.authCaido();
        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));

        EntornoApi.SERVICIOS.resetAll();
        escenarioBase();
        Simulados.deliveryCaido();
        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isServiceUnavailable());

        EntornoApi.SERVICIOS.resetAll();
        escenarioBase();
        EntornoApi.SERVICIOS.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/ubigeo/150101"))
                .willReturn(WireMock.aResponse().withStatus(503)));
        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("CA-4-18 si settings no responde los parámetros salen del caché o de los valores por defecto (30 min)")
    void settingsCaidoUsaDefectos() throws Exception {
        Simulados.settingsCaido();

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiracionMinutos").value(30));
    }

    @Test
    @DisplayName("CA-4-18 una dirección ajena o inexistente responde 404")
    void direccionAjena() throws Exception {
        Simulados.direccionAjena(99);

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":99}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-4-18 sin tarifa de envío para el destino responde 422")
    void sinCobertura() throws Exception {
        Simulados.envioSinCobertura();

        resumen("{\"modalidad\":\"DELIVERY_LOCAL\",\"direccionId\":7}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SIN_COBERTURA"));
    }

    // ---------- HU-4-04: confirmar ----------

    @Test
    @DisplayName("CA-4-19 confirmar crea el pedido FS-… en PENDIENTE_PAGO, reserva el stock, publica PedidoCreado y vacía el carrito")
    void confirmaElPedido() throws Exception {
        Instant antes = Instant.now();

        confirmar("clave-1", delivery("159.72"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codigo").value(org.hamcrest.Matchers.matchesPattern("FS-\\d{6}")))
                .andExpect(jsonPath("$.data.total").value(159.72));

        Pedido pedido = pedidos.findAll().get(0);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(pedido.getClienteId()).isEqualTo(cliente);
        assertThat(Duration.between(antes, pedido.getExpiraEn()).toMinutes()).isBetween(29L, 30L);
        assertThat(pedido.getHistorial()).hasSize(1);
        assertThat(pedido.getHistorial().get(0).getEstadoNuevo()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);

        EntornoApi.SERVICIOS.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/v1/inventario/reservas"))
                .withRequestBody(WireMock.matchingJsonPath("$[?(@.pedidoId == '" + pedido.getId() + "')]"))
                .withRequestBody(WireMock.matchingJsonPath("$.items[?(@.varianteId == 101 && @.cantidad == 2)]")));
        assertThat(eventos("PedidoCreado")).hasSize(1);
        assertThat(datos(eventos("PedidoCreado").get(0)).get("codigo").asString()).isEqualTo(pedido.getCodigo());
        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).isEmpty();
    }

    @Test
    @DisplayName("CA-4-20 confirmar dos veces con la misma clave deja un solo pedido y devuelve el mismo código")
    void idempotencia() throws Exception {
        String primero = cuerpo(confirmar("clave-2", delivery("159.72")).andExpect(status().isCreated()))
                .get("data").get("codigo").asString();
        String segundo = cuerpo(confirmar("clave-2", delivery("159.72")).andExpect(status().isOk()))
                .get("data").get("codigo").asString();

        assertThat(segundo).isEqualTo(primero);
        assertThat(pedidos.count()).isEqualTo(1);
        EntornoApi.SERVICIOS.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/v1/inventario/reservas")));
        assertThat(eventos("PedidoCreado")).hasSize(1);
    }

    @Test
    @DisplayName("CA-4-21 si un precio cambió desde el resumen responde 409 y no crea nada")
    void precioCambiado() throws Exception {
        confirmar("clave-3", delivery("150.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRECIO_CAMBIADO"))
                .andExpect(jsonPath("$.mensaje").value("Los precios cambiaron, revisa tu pedido"));

        assertThat(pedidos.count()).isZero();
        EntornoApi.SERVICIOS.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/v1/inventario/reservas")));
        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).hasSize(2);
    }

    @Test
    @DisplayName("CA-4-22 si una variante se agotó responde 409 \"Sin stock para…\" con las líneas afectadas y no crea nada")
    void sinStockEnLaCotizacion() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2).conEstado("STOCK_INSUFICIENTE", 1),
                Linea.ok(230, "Top Flex", "39.90", 1));

        confirmar("clave-4", delivery("159.72"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_STOCK"))
                .andExpect(jsonPath("$.mensaje").value(startsWith("Sin stock para: Legging Pro")))
                .andExpect(jsonPath("$.detalles.faltantes[0].varianteId").value(101))
                .andExpect(jsonPath("$.detalles.faltantes[0].solicitado").value(2))
                .andExpect(jsonPath("$.detalles.faltantes[0].disponible").value(1));

        assertThat(pedidos.count()).isZero();
    }

    @Test
    @DisplayName("CA-4-22 si catalog rechaza la reserva por falta de stock responde 409 con los faltantes y no crea nada")
    void sinStockEnLaReserva() throws Exception {
        Simulados.reservaSinStock(230, 1, 0);

        confirmar("clave-5", delivery("159.72"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_STOCK"))
                .andExpect(jsonPath("$.mensaje").value(startsWith("Sin stock para: Top Flex")))
                .andExpect(jsonPath("$.detalles.faltantes[0].varianteId").value(230))
                .andExpect(jsonPath("$.detalles.faltantes[0].disponible").value(0));

        assertThat(pedidos.count()).isZero();
        assertThat(eventos("PedidoCreado")).isEmpty();
        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).hasSize(2);
    }

    @Test
    @DisplayName("CA-4-23 el pedido guarda una copia de productos, descuentos, destinatario, dirección con nombres y envío")
    void copiaCongelada() throws Exception {
        Simulados.cotizarEnvio("12.00", 1, 2, null);
        confirmar("clave-6", delivery("159.72")).andExpect(status().isCreated());

        // Aunque después cambie el precio, el nombre o la dirección, el pedido conserva lo que se compró
        EntornoApi.SERVICIOS.resetAll();
        Simulados.cotizar(Linea.ok(101, "Otro nombre", "99.00", 2));
        Simulados.direccion(7, "040101");

        Pedido pedido = pedidos.findAll().get(0);
        assertThat(pedido.getItems()).hasSize(2);
        var legging = pedido.getItems().get(0);
        assertThat(legging.getProductoNombre()).isEqualTo("Legging Pro");
        assertThat(legging.getSku()).isEqualTo("SKU-101");
        assertThat(legging.getTalla()).isEqualTo("M");
        assertThat(legging.getColor()).isEqualTo("Negro");
        assertThat(legging.getImagenUrl()).isEqualTo("http://media/101.jpg");
        assertThat(legging.getPrecioUnitario()).isEqualByComparingTo("59.90");
        assertThat(legging.getDescuentoPorcentaje()).isEqualByComparingTo("10");
        assertThat(legging.getDescuentoMonto()).isEqualByComparingTo("11.98");
        assertThat(legging.getTotal()).isEqualByComparingTo("107.82");
        assertThat(pedido.getDestinatario().getNombres()).isEqualTo("Ana");
        assertThat(pedido.getDestinatario().getDni()).isEqualTo("12345678");
        assertThat(pedido.getDireccion().getUbigeo()).isEqualTo("150101");
        assertThat(pedido.getDireccion().getDepartamento()).isEqualTo("Lima");
        assertThat(pedido.getDireccion().getDistrito()).isEqualTo("Lima");
        assertThat(pedido.getDireccion().getDireccion()).isEqualTo("Jr. Gamarra 123");
        assertThat(pedido.getEnvio().getPlazoDiasMax()).isEqualTo(2);
        assertThat(pedido.getCostoEnvio()).isEqualByComparingTo("12.00");
        assertThat(pedido.getNotas()).isEqualTo("Tocar el timbre");
    }

    @Test
    @DisplayName("CA-4-24 si una dependencia no responde al confirmar responde 503 y no queda ningún pedido")
    void dependenciaCaidaAlConfirmar() throws Exception {
        Simulados.reservaCaida();
        confirmar("clave-7", delivery("159.72")).andExpect(status().isServiceUnavailable());

        EntornoApi.SERVICIOS.resetAll();
        escenarioBase();
        Simulados.catalogCaido();
        confirmar("clave-8", delivery("159.72")).andExpect(status().isServiceUnavailable());

        EntornoApi.SERVICIOS.resetAll();
        escenarioBase();
        Simulados.deliveryCaido();
        confirmar("clave-9", delivery("159.72")).andExpect(status().isServiceUnavailable());

        assertThat(pedidos.count()).isZero();
        assertThat(eventos("PedidoCreado")).isEmpty();
        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).hasSize(2);
    }

    @Test
    @DisplayName("CA-4-24 si catalog no responde en absoluto (conexión cortada) también responde 503")
    void catalogSinRespuesta() throws Exception {
        Simulados.catalogSinRespuesta();

        confirmar("clave-10", delivery("159.72")).andExpect(status().isServiceUnavailable());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    @DisplayName("CA-4-25 el pedido guardado cuadra: total = subtotal − descuentos + envío y cada ítem también")
    void montosDelPedidoGuardado() throws Exception {
        confirmar("clave-11", delivery("159.72")).andExpect(status().isCreated());

        Pedido pedido = pedidos.findAll().get(0);
        assertThat(pedido.getSubtotal()).isEqualByComparingTo("159.70");
        assertThat(pedido.getDescuentoTotal()).isEqualByComparingTo("11.98");
        assertThat(pedido.getTotal()).isEqualByComparingTo(
                pedido.getSubtotal().subtract(pedido.getDescuentoTotal()).add(pedido.getCostoEnvio()));
        pedido.getItems().forEach(item -> assertThat(item.getTotal()).isEqualByComparingTo(
                item.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(item.getCantidad()))
                        .subtract(item.getDescuentoMonto())));
    }

    @Test
    @DisplayName("CA-4-19 sin la cabecera Idempotency-Key no se puede confirmar: 400")
    void sinClave() throws Exception {
        mvc.perform(post("/api/v1/pedidos").with(Sesiones.cliente(cliente)).contentType(APPLICATION_JSON)
                .content(delivery("159.72")))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    @DisplayName("CA-4-19 solo un cliente puede confirmar un pedido: sin sesión 401 y un operador 403")
    void soloClientes() throws Exception {
        mvc.perform(post("/api/v1/pedidos").header("Idempotency-Key", "x").contentType(APPLICATION_JSON)
                .content(delivery("159.72"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pedidos").with(Sesiones.operador()).header("Idempotency-Key", "x")
                .contentType(APPLICATION_JSON).content(delivery("159.72"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/checkout/resumen").contentType(APPLICATION_JSON)
                .content("{\"modalidad\":\"RECOJO\"}")).andExpect(status().isUnauthorized());
    }
}
