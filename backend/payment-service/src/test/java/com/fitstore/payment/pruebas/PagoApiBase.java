package com.fitstore.payment.pruebas;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.when;

import com.fitstore.payment.client.ConfiguracionTienda;
import com.fitstore.payment.dto.ConfiguracionPublica;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base de las pruebas de API de payment: el servicio completo con PostgreSQL real, Mercado Pago simulado con WireMock y
 * settings simulado (Mercado Pago habilitado en la tienda). Sin transacción de prueba: el cobro confirma de verdad,
 * así que cada prueba crea sus propios datos con códigos únicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
public abstract class PagoApiBase {

    @DynamicPropertySource
    static void mercadoPago(DynamicPropertyRegistry registro) {
        WireMockPrueba.registrar(registro);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JsonMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected IntencionPagoRepository intenciones;
    @Autowired protected TransaccionRepository transacciones;

    @MockitoBean protected ConfiguracionTienda tienda;

    @BeforeEach
    void prepararBase() {
        WireMockPrueba.servidor().resetAll();
        when(tienda.metodosPago()).thenReturn(Optional.of(List.of("MERCADO_PAGO")));
        when(tienda.publica()).thenReturn(Optional.of(new ConfiguracionPublica(
                new ConfiguracionPublica.Identidad("Moda Activa", "Moda Activa S.A.C.", "20123456789",
                        "Jr. Gamarra 123, La Victoria", null),
                new ConfiguracionPublica.Parametros(List.of("MERCADO_PAGO")))));
    }

    /** Intención PENDIENTE de S/ 119.80 que vence en 30 minutos, con código único. */
    protected IntencionPago intencion(UUID clienteId) {
        return intencion(clienteId, "119.80", Instant.now().plus(30, ChronoUnit.MINUTES));
    }

    protected IntencionPago intencion(UUID clienteId, String monto, Instant expiraEn) {
        IntencionPago i = Datos.intencion();
        i.setClienteId(clienteId);
        i.setMonto(new BigDecimal(monto));
        i.setSubtotal(new BigDecimal(monto));
        i.setExpiraEn(expiraEn);
        i.setCodigoPedido("FS-%06d".formatted(ThreadLocalRandom.current().nextInt(100_000, 999_999)));
        return intenciones.saveAndFlush(i);
    }

    /** Transacción PROCESANDO de la intención, cuyo último movimiento fue hace {@code minutos} minutos. */
    protected Transaccion transaccionProcesando(IntencionPago intencion, Proveedor proveedor, String idProveedor,
            int minutos) {
        Transaccion t = Datos.transaccion(intencion.getId(), EstadoTransaccion.PROCESANDO, UUID.randomUUID().toString());
        t.setProveedor(proveedor);
        t.setMonto(intencion.getMonto());
        t.setIdProveedor(idProveedor);
        t = transacciones.saveAndFlush(t);
        jdbc.update("UPDATE transaccion SET actualizado_en = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(minutos, ChronoUnit.MINUTES)), t.getId());
        return t;
    }

    protected String estadoTransaccion(UUID transaccionId) {
        return jdbc.queryForObject("SELECT estado FROM transaccion WHERE id = ?", String.class, transaccionId);
    }

    protected int contarTransacciones(UUID intencionId, String estado) {
        return jdbc.queryForObject("SELECT count(*) FROM transaccion WHERE intencion_id = ? AND estado = ?",
                Integer.class, intencionId, estado);
    }

    protected String estadoIntencion(UUID intencionId) {
        return jdbc.queryForObject("SELECT estado FROM intencion_pago WHERE id = ?", String.class, intencionId);
    }

    protected int eventosEnOutbox(UUID pedidoId, String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = ? AND agregado_id = ?",
                Integer.class, tipo, pedidoId.toString());
    }

    // ------------------------------------------------------------ Mercado Pago simulado

    protected static String pagoMp(long id, String estado, String detalle) {
        return """
                {"id":%d,"status":"%s","status_detail":"%s","payment_method_id":"visa","payment_type_id":"credit_card",
                 "card":{"last_four_digits":"1111"},"transaction_amount":119.8,"currency_id":"PEN"}
                """.formatted(id, estado, detalle);
    }

    protected static MappingBuilder crearPagoMp() {
        return post(urlEqualTo("/v1/payments"));
    }

    protected static MappingBuilder consultarPagoMp(long id) {
        return get(urlPathEqualTo("/v1/payments/" + id));
    }

    protected static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder respuesta(int estado, String cuerpo) {
        return aResponse().withStatus(estado).withHeader("Content-Type", "application/json").withBody(cuerpo);
    }
}
