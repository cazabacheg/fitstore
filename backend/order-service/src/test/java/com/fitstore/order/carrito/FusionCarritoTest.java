package com.fitstore.order.carrito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.pruebas.ApiBase;
import com.fitstore.order.pruebas.Sesiones;
import com.fitstore.order.pruebas.Simulados;
import com.fitstore.order.pruebas.Simulados.Linea;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** HU-4-02 Conservar mi carrito al iniciar sesión: POST /carrito/fusionar. */
class FusionCarritoTest extends ApiBase {

    private final UUID clienteId = UUID.randomUUID();
    private final CarritoId cliente = CarritoId.cliente(clienteId);
    private final String anonimoId = UUID.randomUUID().toString();
    private final CarritoId anonimo = CarritoId.anonimo(anonimoId);

    private ResultActions fusionar(String carritoId) throws Exception {
        var peticion = post("/api/v1/carrito/fusionar").with(Sesiones.cliente(clienteId));
        return mvc.perform(carritoId == null ? peticion : peticion.header("X-Carrito-Id", carritoId));
    }

    @Test
    @DisplayName("CA-4-10 suma el carrito anónimo al del cliente, borra el anónimo y devuelve el carrito resultante")
    void sumaYBorraElAnonimo() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 3));
        carritos.establecerLinea(anonimo, 101, 2);
        carritos.establecerLinea(cliente, 101, 1);

        fusionar(anonimoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cantidadTotal").value(3))
                .andExpect(jsonPath("$.data.lineas", hasSize(1)));

        assertThat(carritos.obtenerLineas(cliente)).isEqualTo(Map.of(101L, 3));
        assertThat(carritos.obtenerLineas(anonimo)).isEmpty();
    }

    @Test
    @DisplayName("CA-4-10 si el cliente no tenía carrito, el anónimo pasa a ser suyo")
    void clienteSinCarrito() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2), Linea.ok(230, "Top Flex", "39.90", 1));
        carritos.establecerLinea(anonimo, 101, 2);
        carritos.establecerLinea(anonimo, 230, 1);

        fusionar(anonimoId).andExpect(status().isOk()).andExpect(jsonPath("$.data.cantidadTotal").value(3));

        assertThat(carritos.obtenerLineas(cliente)).isEqualTo(Map.of(101L, 2, 230L, 1));
    }

    @Test
    @DisplayName("CA-4-10 si una suma supera 100 unidades queda en 100 y el carrito trae un aviso")
    void topeDeCienUnidades() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 100));
        carritos.establecerLinea(anonimo, 101, 60);
        carritos.establecerLinea(cliente, 101, 60);

        fusionar(anonimoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avisos", hasItem("La cantidad de Legging Pro se ajustó a 100")));

        assertThat(carritos.obtenerLineas(cliente)).isEqualTo(Map.of(101L, 100));
    }

    @Test
    @DisplayName("CA-4-10 fusionar dos veces no duplica nada: la segunda deja el carrito igual")
    void unaSolaFusion() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 3));
        carritos.establecerLinea(anonimo, 101, 2);
        carritos.establecerLinea(cliente, 101, 1);

        fusionar(anonimoId).andExpect(status().isOk());
        fusionar(anonimoId).andExpect(status().isOk()).andExpect(jsonPath("$.data.cantidadTotal").value(3));

        assertThat(carritos.obtenerLineas(cliente)).isEqualTo(Map.of(101L, 3));
    }

    @Test
    @DisplayName("CA-4-12 sin carrito anónimo (sin cabecera o con un id que no es UUID) devuelve el carrito del cliente")
    void sinAnonimo() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2));
        carritos.establecerLinea(cliente, 101, 2);

        fusionar(null).andExpect(status().isOk()).andExpect(jsonPath("$.data.cantidadTotal").value(2));
        fusionar("no-es-uuid").andExpect(status().isOk()).andExpect(jsonPath("$.data.cantidadTotal").value(2));

        assertThat(carritos.obtenerLineas(cliente)).isEqualTo(Map.of(101L, 2));
    }

    @Test
    @DisplayName("CA-4-12 tras fusionar, GET /carrito del cliente ya trae lo que agregó sin sesión")
    void despuesDeFusionarElCarritoEstaLleno() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2));
        carritos.establecerLinea(anonimo, 101, 2);

        fusionar(anonimoId).andExpect(status().isOk());

        mvc.perform(get("/api/v1/carrito").with(Sesiones.cliente(clienteId)).header("X-Carrito-Id", anonimoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cantidadTotal").value(2))
                .andExpect(jsonPath("$.data.puedeIrAlCheckout").value(true));
    }

    @Test
    @DisplayName("CA-4-12 sin sesión responde 401 y un operador, 403")
    void soloClientes() throws Exception {
        mvc.perform(post("/api/v1/carrito/fusionar").header("X-Carrito-Id", anonimoId))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/carrito/fusionar").with(Sesiones.operador()).header("X-Carrito-Id", anonimoId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CA-4-12 el checkout del cliente deja de responder CARRITO_VACIO cuando el carrito se fusionó antes")
    void checkoutVeElCarritoFusionado() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 2));
        Simulados.perfil(clienteId.toString());
        Simulados.configuracion(List.of("DELIVERY_LOCAL", "AGENCIA", "RECOJO"), 30);
        carritos.establecerLinea(anonimo, 101, 2);

        mvc.perform(post("/api/v1/checkout/resumen").with(Sesiones.cliente(clienteId))
                .contentType(APPLICATION_JSON).content("{\"modalidad\":\"RECOJO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CARRITO_VACIO"));

        fusionar(anonimoId).andExpect(status().isOk());

        mvc.perform(post("/api/v1/checkout/resumen").with(Sesiones.cliente(clienteId))
                .contentType(APPLICATION_JSON).content("{\"modalidad\":\"RECOJO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("CA-4-66 el servidor recalcula el carrito con sus precios y tramos: los montos del navegador se ignoran")
    void ignoraMontosDelNavegador() throws Exception {
        Simulados.cotizar(Linea.ok(101, "Legging Pro", "59.90", 3).conDescuento("10"));
        carritos.establecerLinea(anonimo, 101, 1);

        mvc.perform(put("/api/v1/carrito/items/101").header("X-Carrito-Id", anonimoId)
                .contentType(APPLICATION_JSON)
                .content("{\"cantidad\":3,\"total\":1.00,\"subtotal\":1.00,\"descuentoTotal\":99.00,\"precioUnitario\":0.01}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(179.7))
                .andExpect(jsonPath("$.data.descuentoTotal").value(17.97))
                .andExpect(jsonPath("$.data.total").value(161.73));
    }
}
