package com.fitstore.catalog.inventario.controller;

import static com.fitstore.catalog.pruebas.Sesiones.cliente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.pruebas.DatosConfirmados;
import com.fitstore.catalog.pruebas.PostgresPrueba;
import com.fitstore.catalog.pruebas.Sesiones;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Reserva interna de stock que llama order al crear un pedido (HU-2-10: CA-2-45, 46 y 48). Sin transacción de prueba: la
 * reserva se confirma o se deshace como en producción, y los datos se borran al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ReservaInternaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    DatosConfirmados datos;

    @BeforeEach
    void preparar() {
        datos = new DatosConfirmados(jdbc);
    }

    @AfterEach
    void limpiar() {
        datos.limpiar();
        jdbc.update("UPDATE configuracion_inventario SET stock_minimo_por_defecto = 5, margen_reserva_minutos = 5");
    }

    /** Variante con su stock; el nombre de la talla no importa para la reserva. */
    private long nueva(String codigo, String talla, int fisico, int reservado) {
        return datos.variante(codigo, fisico, reservado);
    }

    private static final String URL = "/internal/v1/inventario/reservas";

    private static String cuerpo(UUID pedidoId, Instant expira, String items) {
        return "{\"pedidoId\":\"" + pedidoId + "\",\"expiraEnPedido\":\"" + expira + "\",\"items\":[" + items + "]}";
    }

    private static String item(long varianteId, int cantidad) {
        return "{\"varianteId\":" + varianteId + ",\"cantidad\":" + cantidad + "}";
    }

    private ResultActions reservar(String json) throws Exception {
        return mvc.perform(post(URL).with(cliente()).contentType(APPLICATION_JSON).content(json));
    }

    private Map<String, Object> stockDe(long varianteId) {
        return jdbc.queryForMap("SELECT fisico, reservado FROM stock WHERE variante_id = ?", varianteId);
    }

    @Test
    @DisplayName("CA-2-45 con stock suficiente reserva todas las líneas y vence en vencimiento del pedido + margen")
    void reservaTodasLasLineas() throws Exception {
        long a = nueva("RES01", "M", 10, 0);
        long b = nueva("RES02", "S", 5, 1);
        UUID pedido = UUID.randomUUID();
        Instant expiraPedido = Instant.parse("2030-01-01T10:30:00Z");

        reservar(cuerpo(pedido, expiraPedido, item(b, 4) + "," + item(a, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pedidoId").value(pedido.toString()))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.expiraEn").value("2030-01-01T10:35:00Z"))
                .andExpect(jsonPath("$.data.items", hasSize(2)));

        assertThat(stockDe(a)).containsEntry("reservado", 3).containsEntry("fisico", 10);
        assertThat(stockDe(b)).containsEntry("reservado", 5).containsEntry("fisico", 5);
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE pedido_id = ?::uuid", String.class, pedido.toString()))
                .isEqualTo("ACTIVA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_item WHERE pedido_id = ?::uuid", Integer.class, pedido.toString()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("CA-2-45 el margen de reserva configurable se suma al vencimiento del pedido")
    void usaElMargenConfigurado() throws Exception {
        long a = nueva("RES03", "M", 10, 0);
        mvc.perform(put("/api/v1/admin/inventario/configuracion").with(Sesiones.admin()).contentType(APPLICATION_JSON)
                        .content("{\"stockMinimoPorDefecto\":5,\"margenReservaMinutos\":12}"))
                .andExpect(status().isOk());

        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expiraEn").value("2030-01-01T10:42:00Z"));
    }

    @Test
    @DisplayName("CA-2-46 si una línea no alcanza no se reserva ninguna y responde 409 con la lista de faltantes")
    void todoONada() throws Exception {
        long alcanza = nueva("RES04", "M", 10, 0);
        long noAlcanza = nueva("RES05", "S", 3, 1);
        long agotada = nueva("RES06", "L", 0, 0);
        UUID pedido = UUID.randomUUID();

        reservar(cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"),
                item(alcanza, 2) + "," + item(noAlcanza, 5) + "," + item(agotada, 1) + "," + item(999999999L, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_STOCK"))
                .andExpect(jsonPath("$.detalles.faltantes", hasSize(3)))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + noAlcanza + ")].solicitado").value(5))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + noAlcanza + ")].disponible").value(2))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==" + agotada + ")].disponible").value(0))
                .andExpect(jsonPath("$.detalles.faltantes[?(@.varianteId==999999999)].disponible").value(0));

        assertThat(stockDe(alcanza)).containsEntry("reservado", 0);
        assertThat(stockDe(noAlcanza)).containsEntry("reservado", 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE pedido_id = ?::uuid", Integer.class, pedido.toString())).isZero();
    }

    @Test
    @DisplayName("CA-2-48 repetir la misma reserva (mismo pedidoId) devuelve la existente con 200 sin reservar dos veces")
    void esIdempotente() throws Exception {
        long a = nueva("RES07", "M", 10, 0);
        UUID pedido = UUID.randomUUID();
        String json = cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"), item(a, 3));

        reservar(json).andExpect(status().isCreated());
        reservar(json).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pedidoId").value(pedido.toString()))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.items[0].cantidad").value(3));
        // aunque el reintento traiga otras cantidades, manda la reserva original
        reservar(cuerpo(pedido, Instant.parse("2030-01-01T10:30:00Z"), item(a, 9))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].cantidad").value(3));

        assertThat(stockDe(a)).containsEntry("reservado", 3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE pedido_id = ?::uuid", Integer.class, pedido.toString())).isOne();
    }

    @Test
    @DisplayName("CA-2-45 una reserva que baja el disponible bajo el mínimo publica StockBajo una vez")
    void reservaCruzaElUmbral() throws Exception {
        long a = nueva("RES08", "M", 8, 0);

        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a, 4))).andExpect(status().isCreated());
        reservar(cuerpo(UUID.randomUUID(), Instant.parse("2030-01-01T10:30:00Z"), item(a, 1))).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'StockBajo' AND agregado_id = ?",
                Integer.class, String.valueOf(a))).isEqualTo(1);
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
