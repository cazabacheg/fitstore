package com.fitstore.order.carrito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.EntornoApi;
import com.fitstore.order.pruebas.Sesiones;
import com.fitstore.order.pruebas.Simulados;
import com.fitstore.order.pruebas.Simulados.Linea;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** HU-4-01 Gestionar mi carrito: carrito anónimo (X-Carrito-Id) y de cliente, cotizado contra catalog. */
class CarritoServiceTest extends ApiBase {

    private final String anonimo = UUID.randomUUID().toString();
    private final CarritoId carrito = CarritoId.anonimo(anonimo);

    private ResultActions agregar(long varianteId, int cantidad) throws Exception {
        return mvc.perform(post("/api/v1/carrito/items").header("X-Carrito-Id", anonimo)
                .contentType(APPLICATION_JSON)
                .content("{\"varianteId\":" + varianteId + ",\"cantidad\":" + cantidad + "}"));
    }

    @Test
    @DisplayName("CA-4-01 agregar una variante que ya estaba suma a la línea y devuelve el carrito con el total de unidades")
    void agregaYSuma() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 3));

        agregar(101, 2).andExpect(status().isOk());
        agregar(101, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cantidadTotal").value(3))
                .andExpect(jsonPath("$.data.lineas", hasSize(1)));

        assertThat(carritos.obtenerLineas(carrito)).isEqualTo(Map.of(101L, 3));
    }

    @Test
    @DisplayName("CA-4-01 el carrito de un cliente con sesión es independiente del anónimo y se guarda por su id")
    void carritoDeCliente() throws Exception {
        UUID cliente = UUID.randomUUID();
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 1));

        mvc.perform(post("/api/v1/carrito/items").with(Sesiones.cliente(cliente)).header("X-Carrito-Id", anonimo)
                .contentType(APPLICATION_JSON).content("{\"varianteId\":101,\"cantidad\":1}"))
                .andExpect(status().isOk());

        assertThat(carritos.obtenerLineas(CarritoId.cliente(cliente))).containsEntry(101L, 1);
        assertThat(carritos.obtenerLineas(carrito)).isEmpty();
    }

    @Test
    @DisplayName("CA-4-02 el carrito muestra cada línea con sus montos, el total sin envío y cuánto falta para el siguiente tramo")
    void muestraMontosYTramo() throws Exception {
        Simulados.cotizar(
                Linea.ok(101, "Legging Pro", "59.90", 4).conDescuento("10"),
                Linea.ok(230, "Top Flex", "39.90", 1));
        carritos.establecerLinea(carrito, 101, 4);
        carritos.establecerLinea(carrito, 230, 1);

        mvc.perform(get("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas", hasSize(2)))
                .andExpect(jsonPath("$.data.lineas[0].producto").value("Legging Pro"))
                .andExpect(jsonPath("$.data.lineas[0].talla").value("M"))
                .andExpect(jsonPath("$.data.lineas[0].color").value("Negro"))
                .andExpect(jsonPath("$.data.lineas[0].imagenUrl").value("http://media/101.jpg"))
                .andExpect(jsonPath("$.data.lineas[0].precioUnitario").value(59.9))
                .andExpect(jsonPath("$.data.lineas[0].descuentoPorcentaje").value(10))
                .andExpect(jsonPath("$.data.lineas[0].descuentoMonto").value(23.96))
                .andExpect(jsonPath("$.data.lineas[0].total").value(215.64))
                .andExpect(jsonPath("$.data.subtotal").value(279.5))
                .andExpect(jsonPath("$.data.descuentoTotal").value(23.96))
                .andExpect(jsonPath("$.data.total").value(255.54))
                .andExpect(jsonPath("$.data.cantidadTotal").value(5))
                .andExpect(jsonPath("$.data.avisosTramo[0].producto").value("Legging Pro"))
                .andExpect(jsonPath("$.data.avisosTramo[0].siguienteTramo.faltan").value(2))
                .andExpect(jsonPath("$.data.avisosTramo[0].siguienteTramo.porcentaje").value(15))
                .andExpect(jsonPath("$.data.preciosPorConfirmar").value(false))
                .andExpect(jsonPath("$.data.puedeIrAlCheckout").value(true));
    }

    @Test
    @DisplayName("CA-4-03 al cambiar la cantidad o quitar una línea se vuelve a cotizar; vaciar deja el carrito sin nada")
    void cambiaQuitaYVacia() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 5), Linea.ok(230, "Top Flex", "39.90", 1));
        carritos.establecerLinea(carrito, 101, 2);
        carritos.establecerLinea(carrito, 230, 1);

        mvc.perform(put("/api/v1/carrito/items/101").header("X-Carrito-Id", anonimo)
                .contentType(APPLICATION_JSON).content("{\"cantidad\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cantidadTotal").value(6));
        assertThat(carritos.obtenerLineas(carrito)).containsEntry(101L, 5);
        EntornoApi.SERVICIOS.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/precios/cotizar"))
                .withRequestBody(WireMock.matchingJsonPath("$.lineas[?(@.varianteId == 101 && @.cantidad == 5)]")));

        mvc.perform(delete("/api/v1/carrito/items/230").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cantidadTotal").value(5));
        assertThat(carritos.obtenerLineas(carrito)).doesNotContainKey(230L);

        mvc.perform(delete("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas", hasSize(0)))
                .andExpect(jsonPath("$.data.cantidadTotal").value(0));
        assertThat(carritos.obtenerLineas(carrito)).isEmpty();
    }

    @Test
    @DisplayName("CA-4-03 cambiar o quitar una línea que no está en el carrito responde 404")
    void lineaInexistente() throws Exception {
        mvc.perform(put("/api/v1/carrito/items/999").header("X-Carrito-Id", anonimo)
                .contentType(APPLICATION_JSON).content("{\"cantidad\":2}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/carrito/items/999").header("X-Carrito-Id", anonimo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-4-04 pedir 5 con 3 disponibles responde 409 \"Solo quedan 3 unidades\" y la línea conserva su cantidad")
    void stockInsuficiente() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 5).conEstado("STOCK_INSUFICIENTE", 3));
        carritos.establecerLinea(carrito, 101, 2);

        agregar(101, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Solo quedan 3 unidades"))
                .andExpect(jsonPath("$.detalles.disponible").value(3));

        assertThat(carritos.obtenerLineas(carrito)).isEqualTo(Map.of(101L, 2));
    }

    @Test
    @DisplayName("CA-4-04 cambiar la cantidad por encima del stock responde 409 y conserva la cantidad anterior")
    void stockInsuficienteAlCambiar() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 5).conEstado("STOCK_INSUFICIENTE", 1));
        carritos.establecerLinea(carrito, 101, 1);

        mvc.perform(put("/api/v1/carrito/items/101").header("X-Carrito-Id", anonimo)
                .contentType(APPLICATION_JSON).content("{\"cantidad\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Solo queda 1 unidad"));

        assertThat(carritos.obtenerLineas(carrito)).isEqualTo(Map.of(101L, 1));
    }

    @Test
    @DisplayName("CA-4-05 más de 50 líneas o de 100 unidades en una línea responde 400 con el límite en el mensaje")
    void limites() throws Exception {
        Simulados.cotizar(Linea.ok(900, "Producto", "10.00", 1));
        for (long v = 1; v <= 50; v++) {
            carritos.establecerLinea(carrito, v, 1);
        }
        agregar(900, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("LIMITE_CARRITO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("50")));

        carritos.establecerLinea(carrito, 7, 60);
        agregar(7, 50)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("LIMITE_CARRITO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("100")));
        assertThat(carritos.obtenerLineas(carrito)).containsEntry(7L, 60).doesNotContainKey(900L);
    }

    @Test
    @DisplayName("CA-4-06 una línea NO_DISPONIBLE se marca y el carrito no puede ir al checkout")
    void lineaNoDisponible() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 1), Linea.ok(230, "Top Flex", "39.90", 1)
                .conEstado("NO_DISPONIBLE", null));
        carritos.establecerLinea(carrito, 101, 1);
        carritos.establecerLinea(carrito, 230, 1);

        mvc.perform(get("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas[1].estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.data.puedeIrAlCheckout").value(false));
    }

    @Test
    @DisplayName("CA-4-07 si catalog no responde se muestra la última cotización con precios por confirmar")
    void catalogCaido() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2));
        carritos.establecerLinea(carrito, 101, 2);
        mvc.perform(get("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(jsonPath("$.data.preciosPorConfirmar").value(false));

        EntornoApi.SERVICIOS.resetAll();
        Simulados.catalogCaido();
        mvc.perform(get("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preciosPorConfirmar").value(true))
                .andExpect(jsonPath("$.data.puedeIrAlCheckout").value(false))
                .andExpect(jsonPath("$.data.lineas[0].precioUnitario").value(59.9))
                .andExpect(jsonPath("$.data.total").value(119.8));
    }

    @Test
    @DisplayName("CA-4-07 agregar con catalog caído responde 503 y no cambia el carrito")
    void agregarConCatalogCaido() throws Exception {
        Simulados.catalogCaido();

        agregar(101, 1).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));
        assertThat(carritos.obtenerLineas(carrito)).isEmpty();
    }

    @Test
    @DisplayName("CA-4-08 un carrito vacío se devuelve sin consultar a catalog")
    void carritoVacio() throws Exception {
        mvc.perform(get("/api/v1/carrito").header("X-Carrito-Id", anonimo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas", hasSize(0)))
                .andExpect(jsonPath("$.data.cantidadTotal").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.puedeIrAlCheckout").value(false));
        EntornoApi.SERVICIOS.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/precios/cotizar")));

        mvc.perform(get("/api/v1/carrito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lineas", hasSize(0)));
    }

    @Test
    @DisplayName("CA-4-01 sin X-Carrito-Id ni sesión no se puede agregar: 400")
    void sinIdentificarElCarrito() throws Exception {
        mvc.perform(post("/api/v1/carrito/items").contentType(APPLICATION_JSON)
                .content("{\"varianteId\":101,\"cantidad\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CARRITO_NO_IDENTIFICADO"));
    }
}
