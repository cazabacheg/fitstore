package com.fitstore.catalog.inventario.controller;

import static com.fitstore.catalog.pruebas.Sesiones.cliente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.ApiBase;
import com.fitstore.catalog.pruebas.Sesiones;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Reserva interna de stock que llama order al crear un pedido (HU-2-10: CA-2-45, 46 y 48). */
class ReservaInternaControllerTest extends ApiBase {

    private static final String URL = "/internal/v1/inventario/reservas";

    private Variante nueva(String codigo, String talla, int fisico, int reservado) {
        Categoria c = categoria("Reserva " + codigo, null);
        Producto p = producto(codigo, "Producto " + codigo, "50.00", EstadoProducto.ACTIVO, c);
        return variante(p, talla, "negro", fisico, reservado);
    }

    private static String cuerpo(UUID pedidoId, Instant expira, String items) {
        return "{\"pedidoId\":\"" + pedidoId + "\",\"expiraEnPedido\":\"" + expira + "\",\"items\":[" + items + "]}";
    }

    private static String item(Long varianteId, int cantidad) {
        return "{\"varianteId\":" + varianteId + ",\"cantidad\":" + cantidad + "}";
    }

    private ResultActions reservar(String json) throws Exception {
        return mvc.perform(post(URL).with(cliente()).contentType(APPLICATION_JSON).content(json));
    }

    private Map<String, Object> stockDe(Long varianteId) {
        return jdbc.queryForMap("SELECT fisico, reservado FROM stock WHERE variante_id = ?", varianteId);
    }

    @Test
    @DisplayName("CA-2-45 con stock suficiente reserva todas las líneas y vence en vencimiento del pedido + margen")
    void reservaTodasLasLineas() throws Exception {
        Variante a = nueva("RES01", "M", 10, 0);
        Variante b = nueva("RES02", "S", 5, 1);
        sincronizar();
        UUID pedido = UUID.randomUUID();
        Instant expiraPedido = Instant.parse("2030-01-01T10:30:00Z");

        reservar(cuerpo(pedido, expiraPedido, item(b.getId(), 4) + "," + item(a.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pedidoId").value(pedido.toString()))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.expiraEn").value("2030-01-01T10:35:00Z"))
                .andExpect(jsonPath("$.data.items", hasSize(2)));

        assertThat(stockDe(a.getId())).containsEntry("reservado", 3).containsEntry("fisico", 10);
        assertThat(stockDe(b.getId())).containsEntry("reservado", 5).containsEntry("fisico", 5);
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE pedido_id = ?::uuid", String.class, pedido.toString()))
                .isEqualTo("ACTIVA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_item WHERE pedido_id = ?::uuid", Integer.class, pedido.toString()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CA-2-45 el margen de reserva configurable se suma al vencimiento del pedido")
    void usaElMargenConfigurado() throws Exception {
        Variante a = nueva("RES03", "M", 10, 0);
        sincronizar();
        mvc.perform(put("/api/v1/admin/inventario/configuracion").with(Sesiones.admin()).contentType(APPLICATION_JSON)
                        .content("{\"stockMinimoPorDefecto\":5,\"margenReservaMinutos\":12}"))
                .andExpect(status().isOk());
        sincronizar();

        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a.getId(), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expiraEn").value("2030-01-01T10:42:00Z"));
    }

    @Test
    @DisplayName("CA-2-46 si una línea no alcanza no se reserva ninguna y responde 409 con la lista de faltantes")
    void todoONada() throws Exception {
        Variante alcanza = nueva("RES04", "M", 10, 0);
        Variante noAlcanza = nueva("RES05", "S", 3, 1);
        Variante agotada = nueva("RES06", "L", 0, 0);
        sincronizar();
        UUID pedido = UUID.randomUUID();

        reservar(cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"),
                item(alcanza.getId(), 2) + "," + item(noAlcanza.getId(), 5) + "," + item(agotada.getId(), 1) + "," + item(999999999L, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_STOCK"))
                .andExpect(jsonPath("$.detalles.faltantes", hasSize(3)))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + noAlcanza.getId() + ")].solicitado").value(5))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + noAlcanza.getId() + ")].disponible").value(2))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + agotada.getId() + ")].disponible").value(0))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==999999999)].disponible").value(0));

        assertThat(stockDe(alcanza.getId())).containsEntry("reservado", 0);
        assertThat(stockDe(noAlcanza.getId())).containsEntry("reservado", 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE pedido_id = ?::uuid", Integer.class, pedido.toString())).isZero();
    }

    @Test
    @DisplayName("CA-2-48 repetir la misma reserva (mismo pedidoId) devuelve la existente con 200 sin reservar dos veces")
    void esIdempotente() throws Exception {
        Variante a = nueva("RES07", "M", 10, 0);
        sincronizar();
        UUID pedido = UUID.randomUUID();
        String json = cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"), item(a.getId(), 3));

        reservar(json).andExpect(status().isCreated());
        reservar(json).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pedidoId").value(pedido.toString()))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.items[0].cantidad").value(3));
        // aunque el reintento traiga otras cantidades, manda la reserva original
        reservar(cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"), item(a.getId(), 9))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].cantidad").value(3));

        assertThat(stockDe(a.getId())).containsEntry("reservado", 3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE pedido_id = ?::uuid", Integer.class, pedido.toString())).isOne();
    }

    @Test
    @DisplayName("CA-2-45 una reserva que baja el disponible bajo el mínimo publica StockBajo una vez")
    void reservaCruzaElUmbral() throws Exception {
        Variante a = nueva("RES08", "M", 8, 0);
        sincronizar();

        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a.getId(), 4))).andExpect(status().isCreated());
        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a.getId(), 1))).andExpect(status().isCreated());

        em.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'StockBajo' AND agregado_id = ?",
                Integer.class, String.valueOf(a.getId()))).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-2-46 valida el cuerpo: sin líneas, cantidad cero, más de 50 líneas o sin pedidoId responde 400")
    void validaElCuerpo() throws Exception {
        Instant expira = Instant.parse("2030-01-01T10:30:00Z");
        reservar(cuerpo(UUID.randomUUID(), expira, "")).andExpect(status().isBadRequest());
        reservar(cuerpo(UUID.randomUUID(), expira, item(1L, 0))).andExpect(status().isBadRequest());
        StringBuilder muchas = new StringBuilder();
        for (int i = 1; i <= 51; i++) {
            muchas.append(i > 1 ? "," : "").append(item((long) i, 1));
        }
        reservar(cuerpo(UUID.randomUUID(), expira, muchas.toString())).andExpect(status().isBadRequest());
        reservar("{\"expiraEnPedido\":\"" + expira + "\",\"items\":[" + item(1L, 1) + "]}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-2-54 la ruta interna exige un JWT válido: sin sesión responde 401")
    void exigeJwt() throws Exception {
        mvc.perform(post(URL).contentType(APPLICATION_JSON)
                        .content(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(1L, 1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
