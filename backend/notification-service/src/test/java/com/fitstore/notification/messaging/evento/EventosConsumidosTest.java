package com.fitstore.notification.messaging.evento;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.model.Modalidad;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los records de los 17 eventos que notification consume (micro 6) leen el {@code datos} del sobre tal como lo
 * define el AsyncAPI, e ignoran los campos que no conocen (compatibilidad hacia adelante, K12).
 */
class EventosConsumidosTest {

    private static final String PEDIDO = "11111111-1111-1111-1111-111111111111";
    private static final String PERSONA = "{\"nombre\":\"Lucía Quispe\",\"email\":\"lucia@correo.pe\"}";
    private static final String DESTINATARIO = "{\"nombres\":\"Lucía\",\"apellidos\":\"Quispe\",\"dni\":\"12345678\","
            + "\"telefono\":\"987654321\",\"email\":\"lucia@correo.pe\"}";

    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private <T> T leer(String datos, Class<T> tipo) {
        return json.readValue(datos, tipo);
    }

    @Test
    @DisplayName("CA-0a-06 los eventos de auth se leen del sobre")
    void eventosDeAuth() {
        var registrado = leer("{\"clienteId\":\"" + PEDIDO + "\",\"email\":\"a@correo.pe\",\"nombres\":\"Ana\"}",
                ClienteRegistrado.class);
        assertThat(registrado.nombres()).isEqualTo("Ana");
        assertThat(registrado.clienteId()).isEqualTo(UUID.fromString(PEDIDO));

        var reset = leer("{\"cuentaId\":\"" + PEDIDO + "\",\"tipoCuenta\":\"STAFF\",\"email\":\"a@correo.pe\","
                + "\"nombres\":\"Ana\",\"motivo\":\"INVITACION\",\"enlace\":\"https://tienda.pe/x?t=1\","
                + "\"expiraEn\":\"2026-10-20T15:00:00Z\",\"campoNuevo\":1}", PasswordResetSolicitado.class);
        assertThat(reset.motivo()).isEqualTo("INVITACION");
        assertThat(reset.expiraEn()).isEqualTo(Instant.parse("2026-10-20T15:00:00Z"));

        var cambiado = leer("{\"cuentaId\":\"" + PEDIDO + "\",\"tipoCuenta\":\"CLIENTE\",\"email\":\"a@correo.pe\","
                + "\"nombres\":\"Ana\"}", PasswordCambiado.class);
        assertThat(cambiado.tipoCuenta()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("CA-0a-06 los eventos de catalog se leen del sobre")
    void eventosDeCatalog() {
        var bajo = leer("{\"varianteId\":12,\"sku\":\"LEG-NEG-M\",\"producto\":\"Legging\",\"talla\":\"M\","
                + "\"color\":\"Negro\",\"disponible\":2,\"minimo\":5}", StockBajo.class);
        assertThat(bajo.disponible()).isEqualTo(2);
        assertThat(bajo.minimo()).isEqualTo(5);

        var sinStock = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"faltantes\":[{\"varianteId\":12,\"sku\":\"LEG-NEG-M\","
                + "\"solicitado\":3,\"disponible\":1}]}", StockNoConfirmado.class);
        assertThat(sinStock.faltantes()).singleElement().satisfies(f -> {
            assertThat(f.solicitado()).isEqualTo(3);
            assertThat(f.disponible()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("CA-0a-06 los eventos de order se leen del sobre")
    void eventosDeOrder() {
        var creado = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigo\":\"FS-000124\",\"clienteId\":\"" + PEDIDO
                + "\",\"destinatario\":" + DESTINATARIO + ",\"modalidad\":\"AGENCIA\",\"direccion\":{\"ubigeo\":\"150101\","
                + "\"departamento\":\"Lima\",\"provincia\":\"Lima\",\"distrito\":\"Lima\",\"direccion\":\"Jr. Gamarra 700\","
                + "\"referencia\":null},\"items\":[{\"varianteId\":12,\"sku\":\"LEG-NEG-M\",\"producto\":\"Legging\","
                + "\"talla\":\"M\",\"color\":\"Negro\",\"imagenUrl\":null,\"precioUnitario\":59.90,\"cantidad\":2,"
                + "\"descuentoPorcentaje\":0,\"descuentoMonto\":0.00,\"total\":119.80}],\"subtotal\":119.80,"
                + "\"descuentoTotal\":0.00,\"costoEnvio\":10.00,\"total\":129.80,\"expiraEn\":\"2026-10-19T16:00:00Z\"}",
                PedidoCreado.class);
        assertThat(creado.modalidad()).isEqualTo(Modalidad.AGENCIA);
        assertThat(creado.destinatario().nombres()).isEqualTo("Lucía");
        assertThat(creado.direccion().distrito()).isEqualTo("Lima");
        assertThat(creado.items()).singleElement().satisfies(i -> {
            assertThat(i.sku()).isEqualTo("LEG-NEG-M");
            assertThat(i.total()).isEqualByComparingTo(new BigDecimal("119.80"));
        });
        assertThat(creado.total()).isEqualByComparingTo("129.80");

        var pagado = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigo\":\"FS-000124\",\"items\":[{\"varianteId\":12,"
                + "\"cantidad\":2}]}", PedidoPagado.class);
        assertThat(pagado.items()).singleElement().satisfies(i -> assertThat(i.cantidad()).isEqualTo(2));

        var cancelado = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigo\":\"FS-000124\",\"fuePagado\":true,"
                + "\"motivo\":null,\"actor\":\"CLIENTE\",\"items\":[]}", PedidoCancelado.class);
        assertThat(cancelado.fuePagado()).isTrue();
        assertThat(cancelado.motivo()).isNull();

        var listo = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigo\":\"FS-000124\",\"destinatario\":" + DESTINATARIO + "}",
                PedidoListoRecojo.class);
        var recogido = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigo\":\"FS-000124\",\"destinatario\":" + DESTINATARIO
                + "}", PedidoRecogido.class);
        assertThat(listo.destinatario().email()).isEqualTo(recogido.destinatario().email());
    }

    @Test
    @DisplayName("CA-0a-06 los eventos de payment se leen del sobre")
    void eventosDePayment() {
        var boleta = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"numero\":\"B001-00000001\",\"url\":\"/api/v1/pagos/x/boleta\"}",
                BoletaEmitida.class);
        assertThat(boleta.numero()).isEqualTo("B001-00000001");

        var reembolso = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigoPedido\":\"FS-000124\",\"solicitudId\":null,"
                + "\"monto\":50.00,\"parcial\":true,\"motivo\":\"POSTVENTA\",\"manual\":false,\"cliente\":" + PERSONA + "}",
                PagoReembolsado.class);
        assertThat(reembolso.parcial()).isTrue();
        assertThat(reembolso.solicitudId()).isNull();
        assertThat(reembolso.cliente().nombre()).isEqualTo("Lucía Quispe");

        var fallido = leer("{\"pedidoId\":\"" + PEDIDO + "\",\"codigoPedido\":\"FS-000124\",\"monto\":50.00,"
                + "\"ultimoError\":\"timeout\",\"intentos\":3}", ReembolsoFallido.class);
        assertThat(fallido.intentos()).isEqualTo(3);
        assertThat(fallido.ultimoError()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("CA-0a-06 EntregaActualizada se lee del sobre con sus campos opcionales")
    void entregaActualizada() {
        var entrega = leer("{\"entregaId\":\"" + PEDIDO + "\",\"pedidoId\":\"" + PEDIDO + "\",\"codigoPedido\":\"FS-000124\","
                + "\"modalidad\":\"AGENCIA\",\"estado\":\"ENVIADO_AGENCIA\",\"intento\":1,\"fechaProgramada\":\"2026-10-22\","
                + "\"repartidorNombre\":null,\"agenciaNombre\":\"Shalom\",\"codigoSeguimiento\":\"SH123\","
                + "\"claveRecojo\":null,\"urlSeguimiento\":\"https://shalom.pe/s/SH123\",\"observacion\":null,"
                + "\"destinatario\":" + PERSONA + "}", EntregaActualizada.class);
        assertThat(entrega.estado()).isEqualTo("ENVIADO_AGENCIA");
        assertThat(entrega.fechaProgramada()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(entrega.agenciaNombre()).isEqualTo("Shalom");
        assertThat(entrega.repartidorNombre()).isNull();
    }

    @Test
    @DisplayName("CA-0a-06 los eventos de settings se leen del sobre")
    void eventosDeSettings() {
        var config = leer("{\"version\":3,\"secciones\":[\"TEMA\"],\"configuracion\":{\"identidad\":{\"nombre\":\"FitStore\"}},"
                + "\"correosAlertas\":[\"ventas@tienda.pe\"]}", ConfiguracionActualizada.class);
        assertThat(config.version()).isEqualTo(3);
        assertThat(config.correosAlertas()).containsExactly("ventas@tienda.pe");
        assertThat(config.configuracion()).containsKey("identidad");

        var registrado = leer("{\"numero\":\"RC-2026-000001\",\"tipo\":\"RECLAMO\",\"consumidor\":" + PERSONA
                + ",\"bien\":{\"tipo\":\"PRODUCTO\"},\"detalle\":\"No llegó\",\"pedidoConsumidor\":\"Quiero mi dinero\","
                + "\"fechaLimite\":\"2026-11-03\",\"creadoEn\":\"2026-10-19T15:00:00Z\"}", ReclamoRegistrado.class);
        assertThat(registrado.fechaLimite()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(registrado.bien()).containsEntry("tipo", "PRODUCTO");

        var respondido = leer("{\"numero\":\"RC-2026-000001\",\"consumidor\":" + PERSONA + ",\"respuesta\":\"Lo sentimos\","
                + "\"respondidoEn\":\"2026-10-20T15:00:00Z\"}", ReclamoRespondido.class);
        assertThat(respondido.respuesta()).isEqualTo("Lo sentimos");
    }
}
