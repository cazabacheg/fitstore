package com.fitstore.notification.envio;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.pruebas.ConfiguracionDeTiempo;
import com.fitstore.notification.pruebas.ConfiguracionDeTiempo.PausaRegistrada;
import com.fitstore.notification.pruebas.LimpiezaBd;
import com.fitstore.notification.pruebas.PostgresInicializador;
import com.fitstore.notification.pruebas.RelojDePrueba;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El despachador envía los PENDIENTE a ritmo controlado y reintenta a los 1, 5, 15 y 60 minutos (SPEC-7 CA-7-08 a
 * CA-7-11). Reloj simulado y pausa registrada; el SMTP es GreenMail, que se detiene para simular la caída.
 */
@SpringBootTest
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
@Import(ConfiguracionDeTiempo.class)
class DespachadorCorreosTest {

    @RegisterExtension
    GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired DespachadorCorreos despachador;
    @Autowired EnvioCorreoRepository envios;
    @Autowired JdbcTemplate jdbc;
    @Autowired RelojDePrueba reloj;
    @Autowired PausaRegistrada pausa;
    @Autowired TransactionTemplate transaccion;

    @BeforeEach
    void preparar() {
        LimpiezaBd.limpiar(jdbc);
        pausa.limpiar();
    }

    /** Las pruebas comparten un solo PostgreSQL con las de repositorio, que esperan la base recién migrada. */
    @AfterEach
    void dejarLaBaseComoLaMigracion() {
        LimpiezaBd.limpiar(jdbc);
    }

    private EnvioCorreo pendiente(String destinatario) {
        EnvioCorreo e = new EnvioCorreo();
        e.setId(UUID.randomUUID());
        e.setEventoId(UUID.randomUUID());
        e.setPlantilla("BIENVENIDA");
        e.setDestinatario(destinatario);
        e.setAsunto("Te damos la bienvenida a FitStore, Ana");
        e.setVariables(Map.of("nombre", "Ana"));
        e.setEstado(EstadoEnvio.PENDIENTE);
        e.setIntentos(0);
        e.setHistorialIntentos(new ArrayList<>());
        e.setProximoIntentoEn(reloj.instant());
        e.setReferencia(null);
        e.setCreadoEn(reloj.instant());
        return envios.saveAndFlush(e);
    }

    private EnvioCorreo recargar(EnvioCorreo e) {
        return envios.findById(e.getId()).orElseThrow();
    }

    @Test
    @DisplayName("CA-7-08 envía los PENDIENTE, los marca ENVIADO y el correo llega con HTML y texto plano")
    void enviaYMarcaEnviado() throws Exception {
        EnvioCorreo envio = pendiente("ana@fitstore.test");

        despachador.despachar();

        EnvioCorreo enviado = recargar(envio);
        assertThat(enviado.getEstado()).isEqualTo(EstadoEnvio.ENVIADO);
        assertThat(enviado.getEnviadoEn()).isEqualTo(reloj.instant());
        assertThat(enviado.getIntentos()).isEqualTo(1);
        MimeMessage[] recibidos = smtp.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos[0].getSubject()).isEqualTo("Te damos la bienvenida a FitStore, Ana");
        assertThat(recibidos[0].getAllRecipients()[0].toString()).isEqualTo("ana@fitstore.test");
        assertThat(recibidos[0].getFrom()[0].toString()).contains("tienda@fitstore.test");
        String contenido = com.icegreen.greenmail.util.GreenMailUtil.getWholeMessage(recibidos[0]);
        assertThat(contenido).contains("text/html").contains("text/plain");
    }

    @Test
    @DisplayName("CA-7-08 envía como máximo el ritmo configurado por segundo y deja el resto para el siguiente ciclo")
    void respetaElRitmo() {
        // ritmo-por-segundo = 2 en las pruebas y el ciclo dura 5 s: caben 10 correos por ciclo, con 500 ms entre uno y otro
        for (int i = 0; i < 12; i++) {
            pendiente("cliente" + i + "@fitstore.test");
        }

        despachador.despachar();

        assertThat(smtp.getReceivedMessages()).hasSize(10);
        assertThat(envios.findAll().stream().filter(e -> e.getEstado() == EstadoEnvio.PENDIENTE)).hasSize(2);
        assertThat(pausa.esperas()).isNotEmpty().allMatch(ms -> ms == 500L);

        despachador.despachar();

        assertThat(smtp.getReceivedMessages()).hasSize(12);
    }

    @Test
    @DisplayName("CA-7-08 dos despachadores a la vez no toman el mismo envío (SKIP LOCKED)")
    void skipLocked() throws Exception {
        for (int i = 0; i < 4; i++) {
            pendiente("cliente" + i + "@fitstore.test");
        }
        CountDownLatch tomado = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);

        CompletableFuture<List<UUID>> primero = CompletableFuture.supplyAsync(() -> transaccion.execute(t -> {
            List<UUID> ids = envios.tomarPendientes(reloj.instant(), 2).stream().map(EnvioCorreo::getId).toList();
            tomado.countDown();
            try {
                soltar.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ids;
        }));
        assertThat(tomado.await(10, TimeUnit.SECONDS)).isTrue();
        List<UUID> segundo = transaccion.execute(t ->
                envios.tomarPendientes(reloj.instant(), 4).stream().map(EnvioCorreo::getId).toList());
        soltar.countDown();

        assertThat(segundo).hasSize(2).doesNotContainAnyElementsOf(primero.get(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("CA-7-09 con el SMTP caído reintenta a los 1, 5, 15 y 60 minutos y luego queda FALLIDO con el último error")
    void reintentosHastaFallido() {
        EnvioCorreo envio = pendiente("ana@fitstore.test");
        smtp.stop();
        List<Duration> esperas = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
                Duration.ofMinutes(60));

        for (int i = 0; i < esperas.size(); i++) {
            despachador.despachar();
            EnvioCorreo actual = recargar(envio);
            assertThat(actual.getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(actual.getIntentos()).isEqualTo(i + 1);
            assertThat(actual.getProximoIntentoEn()).isEqualTo(reloj.instant().plus(esperas.get(i)));
            assertThat(actual.getUltimoError()).isNotBlank();
            // Antes de la hora del reintento no se vuelve a intentar
            despachador.despachar();
            assertThat(recargar(envio).getIntentos()).isEqualTo(i + 1);
            reloj.avanzar(esperas.get(i));
        }

        despachador.despachar();

        EnvioCorreo fallido = recargar(envio);
        assertThat(fallido.getEstado()).isEqualTo(EstadoEnvio.FALLIDO);
        assertThat(fallido.getIntentos()).isEqualTo(5);
        assertThat(fallido.getUltimoError()).isNotBlank();
        assertThat(fallido.getProximoIntentoEn()).isNull();
        assertThat(fallido.getHistorialIntentos()).hasSize(5);
        assertThat(fallido.getHistorialIntentos().get(4)).containsKeys("numero", "fecha", "error");
        // Un FALLIDO no se toma solo
        reloj.avanzar(Duration.ofHours(3));
        despachador.despachar();
        assertThat(recargar(envio).getIntentos()).isEqualTo(5);
    }

    @Test
    @DisplayName("CA-7-09 si el SMTP vuelve, el reintento envía el correo y lo marca ENVIADO")
    void elReintentoEnvia() {
        EnvioCorreo envio = pendiente("ana@fitstore.test");
        smtp.stop();
        despachador.despachar();
        assertThat(recargar(envio).getEstado()).isEqualTo(EstadoEnvio.PENDIENTE);
        smtp.start();
        reloj.avanzar(Duration.ofMinutes(1));

        despachador.despachar();

        EnvioCorreo enviado = recargar(envio);
        assertThat(enviado.getEstado()).isEqualTo(EstadoEnvio.ENVIADO);
        assertThat(enviado.getHistorialIntentos()).hasSize(2);
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("CA-7-11 un envío ENVIADO nunca se vuelve a enviar solo")
    void enviadoNoSeReenviaSolo() {
        EnvioCorreo envio = pendiente("ana@fitstore.test");
        despachador.despachar();
        assertThat(recargar(envio).getEstado()).isEqualTo(EstadoEnvio.ENVIADO);

        reloj.avanzar(Duration.ofDays(1));
        despachador.despachar();
        despachador.despachar();

        assertThat(smtp.getReceivedMessages()).hasSize(1);
        assertThat(recargar(envio).getIntentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-7-08 un reenvío vuelve a renderizar con los textos vigentes (N5)")
    void reenvioUsaLosTextosActuales() throws Exception {
        EnvioCorreo envio = pendiente("ana@fitstore.test");
        despachador.despachar();
        jdbc.update("UPDATE plantilla SET titulo = 'Título nuevo de la bienvenida' WHERE codigo = 'BIENVENIDA'");
        EnvioCorreo enviado = recargar(envio);
        enviado.setEstado(EstadoEnvio.PENDIENTE);
        enviado.setIntentos(0);
        enviado.setProximoIntentoEn(Instant.now());
        envios.saveAndFlush(enviado);

        try {
            despachador.despachar();

            assertThat(smtp.getReceivedMessages()).hasSize(2);
            assertThat(textoDe(smtp.getReceivedMessages()[1])).contains("Título nuevo de la bienvenida");
        } finally {
            jdbc.update("UPDATE plantilla SET titulo = 'Qué gusto tenerte con nosotros' WHERE codigo = 'BIENVENIDA'");
        }
    }

    /** Texto de todas las partes (el cuerpo viaja codificado en quoted-printable o base64). */
    private static String textoDe(jakarta.mail.Part parte) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (parte.isMimeType("multipart/*")) {
            var multi = (jakarta.mail.Multipart) parte.getContent();
            for (int i = 0; i < multi.getCount(); i++) {
                sb.append(textoDe(multi.getBodyPart(i)));
            }
        } else if (parte.isMimeType("text/*")) {
            sb.append(parte.getContent());
        }
        return sb.toString();
    }
}
