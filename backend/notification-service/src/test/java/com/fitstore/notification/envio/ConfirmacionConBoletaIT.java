package com.fitstore.notification.envio;

import static com.fitstore.notification.pruebas.EventosDePrueba.CODIGO;
import static com.fitstore.notification.pruebas.EventosDePrueba.PEDIDO;
import static com.fitstore.notification.pruebas.EventosDePrueba.pedidoCreado;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.common.event.SobreEvento;
import com.fitstore.notification.messaging.EnrutadorEventos;
import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.pruebas.ConfiguracionDeTiempo;
import com.fitstore.notification.pruebas.EventosDePrueba;
import com.fitstore.notification.pruebas.LimpiezaBd;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.pruebas.RelojDePrueba;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * "¡Pedido confirmado!" con la boleta adjunta, descargada de payment (WireMock) al enviar, y el respaldo de 10 minutos
 * sin segunda confirmación (SPEC-7 CA-7-12 a CA-7-14).
 */
@SpringBootTest
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
@Import(ConfiguracionDeTiempo.class)
class ConfirmacionConBoletaIT {

    private static final String NUMERO = "B001-00000124";
    private static final byte[] PDF = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n% boleta de prueba FS-000124\n%%EOF"
            .getBytes(StandardCharsets.ISO_8859_1); // como todo PDF real, lleva bytes no ASCII en la segunda línea
    private static final WireMockServer PAYMENT = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        PAYMENT.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("fitstore.notificacion.payment-url", PAYMENT::baseUrl);
    }

    @RegisterExtension
    GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired EnrutadorEventos enrutador;
    @Autowired DespachadorCorreos despachador;
    @Autowired TemporizadorConfirmacion temporizador;
    @Autowired EnvioCorreoRepository envios;
    @Autowired JdbcTemplate jdbc;
    @Autowired RelojDePrueba reloj;

    @BeforeEach
    void preparar() {
        LimpiezaBd.limpiar(jdbc);
        PAYMENT.resetAll();
        PAYMENT.stubFor(get(urlEqualTo("/internal/v1/boletas/" + NUMERO))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(PDF)));
    }

    /** Las pruebas comparten un solo PostgreSQL con las de repositorio, que esperan la base recién migrada. */
    @AfterEach
    void dejarLaBaseComoLaMigracion() {
        LimpiezaBd.limpiar(jdbc);
    }

    @AfterAll
    static void detener() {
        PAYMENT.stop();
    }

    private void entregar(String tipo, Map<String, Object> datos) {
        SobreEvento sobre = EventosDePrueba.sobre(tipo, datos);
        enrutador.procesar(sobre);
    }

    private void pedidoPagado() {
        entregar("PedidoCreado", pedidoCreado("DELIVERY_LOCAL"));
        entregar("PedidoPagado", Map.of("pedidoId", PEDIDO.toString(), "codigo", CODIGO, "items", List.of()));
    }

    private void boletaEmitida() {
        entregar("BoletaEmitida", Map.of("pedidoId", PEDIDO.toString(), "numero", NUMERO,
                "url", "/api/v1/pagos/FS-000124/boleta"));
    }

    private static byte[] adjunto(MimeMessage mensaje) throws Exception {
        List<Part> adjuntos = new ArrayList<>();
        recolectar(mensaje, adjuntos);
        assertThat(adjuntos).hasSize(1);
        assertThat(adjuntos.get(0).getFileName()).isEqualTo(NUMERO + ".pdf");
        assertThat(adjuntos.get(0).getContentType()).startsWith("application/pdf");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        adjuntos.get(0).getInputStream().transferTo(salida);
        return salida.toByteArray();
    }

    private static void recolectar(Part parte, List<Part> adjuntos) throws Exception {
        if (parte.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) parte.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                BodyPart hija = multi.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(hija.getDisposition())) {
                    adjuntos.add(hija);
                } else {
                    recolectar(hija, adjuntos);
                }
            }
        }
    }

    @Test
    @DisplayName("CA-7-12 BoletaEmitida envía '¡Pedido confirmado!' con el resumen, la modalidad y la boleta PDF adjunta")
    void confirmacionConAdjunto() throws Exception {
        pedidoPagado();
        boletaEmitida();

        despachador.despachar();

        MimeMessage[] recibidos = smtp.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos[0].getSubject()).contains("Pedido " + CODIGO).contains("Ana");
        assertThat(adjunto(recibidos[0])).isEqualTo(PDF);
        String contenido = GreenMailUtil.getWholeMessage(recibidos[0]);
        // El cuerpo es quoted-printable o base64 según el cliente: se decodifica cada parte de texto
        String cuerpo = textoDe(recibidos[0]);
        assertThat(contenido).contains("multipart/");
        assertThat(cuerpo).contains("Legging Power").contains("Jogger Flex").contains("S/ 299.70")
                .contains("Delivery a domicilio").contains("¡Pedido confirmado!");
        assertThat(cuerpo).doesNotContain("70123499");
        EnvioCorreo envio = envios.findAll().get(0);
        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ENVIADO);
        assertThat(envio.getAdjunto()).isEqualTo(NUMERO);
    }

    private static String textoDe(Part parte) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (parte.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) parte.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                sb.append(textoDe(multi.getBodyPart(i)));
            }
        } else if (parte.isMimeType("text/*")) {
            sb.append(parte.getContent());
        }
        return sb.toString();
    }

    @Test
    @DisplayName("CA-7-13 si payment no responde al descargar la boleta, el envío se reintenta como cualquier fallo")
    void paymentCaido() {
        pedidoPagado();
        boletaEmitida();
        PAYMENT.stubFor(get(urlEqualTo("/internal/v1/boletas/" + NUMERO)).willReturn(aResponse().withStatus(503)));

        despachador.despachar();

        EnvioCorreo envio = envios.findAll().get(0);
        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(envio.getIntentos()).isEqualTo(1);
        assertThat(envio.getUltimoError()).isNotBlank();
        assertThat(envio.getProximoIntentoEn()).isEqualTo(reloj.instant().plus(Duration.ofMinutes(1)));
        assertThat(smtp.getReceivedMessages()).isEmpty();

        PAYMENT.stubFor(get(urlEqualTo("/internal/v1/boletas/" + NUMERO))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(PDF)));
        reloj.avanzar(Duration.ofMinutes(1));
        despachador.despachar();

        assertThat(envios.findAll().get(0).getEstado()).isEqualTo(EstadoEnvio.ENVIADO);
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("CA-7-14 10 minutos después de PedidoPagado sin boleta se envía la confirmación sin adjunto, y no se repite")
    void respaldoSinBoleta() throws Exception {
        pedidoPagado();

        reloj.avanzar(Duration.ofMinutes(9));
        temporizador.revisar();
        assertThat(envios.count()).isZero();

        reloj.avanzar(Duration.ofMinutes(1));
        temporizador.revisar();
        temporizador.revisar();

        assertThat(envios.count()).isEqualTo(1);
        EnvioCorreo respaldo = envios.findAll().get(0);
        assertThat(respaldo.getPlantilla()).isEqualTo("PEDIDO_CONFIRMADO");
        assertThat(respaldo.getAdjunto()).isNull();
        despachador.despachar();
        MimeMessage[] recibidos = smtp.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        assertThat(textoDe(recibidos[0])).contains("tu boleta estará disponible en Mis pedidos");
        List<Part> adjuntos = new ArrayList<>();
        recolectar(recibidos[0], adjuntos);
        assertThat(adjuntos).isEmpty();

        // La boleta llega tarde: no se manda una segunda confirmación
        boletaEmitida();
        despachador.despachar();
        assertThat(envios.count()).isEqualTo(1);
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("CA-7-14 si la boleta llega antes de los 10 minutos, el respaldo no envía nada más")
    void boletaAntesDelRespaldo() {
        pedidoPagado();
        boletaEmitida();

        reloj.avanzar(Duration.ofMinutes(30));
        temporizador.revisar();

        assertThat(envios.count()).isEqualTo(1);
        assertThat(envios.findAll().get(0).getAdjunto()).isEqualTo(NUMERO);
    }
}
