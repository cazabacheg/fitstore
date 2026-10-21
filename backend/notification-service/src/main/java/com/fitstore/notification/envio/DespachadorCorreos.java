package com.fitstore.notification.envio;

import com.fitstore.notification.client.PaymentClient;
import com.fitstore.notification.config.PropiedadesNotificacion;
import com.fitstore.notification.envio.MensajeCorreo.AdjuntoCorreo;
import com.fitstore.notification.model.EnvioCorreo;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.CorreoRenderizado;
import com.fitstore.notification.plantillas.MarcaService;
import com.fitstore.notification.plantillas.MarcaVista;
import com.fitstore.notification.plantillas.RenderizadorCorreo;
import com.fitstore.notification.plantillas.TextosPlantilla;
import com.fitstore.notification.repository.EnvioCorreoRepository;
import com.fitstore.notification.repository.PlantillaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía los correos PENDIENTE a ritmo controlado y reintenta los que fallan (micro D6 y D7). Cada ciclo toma un lote con
 * {@code SELECT … FOR UPDATE SKIP LOCKED}, así que dos instancias no repiten un envío. Renderiza al enviar, con la marca
 * y los textos vigentes, y descarga la boleta de payment si el envío la lleva.
 */
@Component
public class DespachadorCorreos {

    private static final Logger LOG = LoggerFactory.getLogger(DespachadorCorreos.class);

    private final EnvioCorreoRepository envios;
    private final PlantillaRepository plantillas;
    private final RenderizadorCorreo renderizador;
    private final MarcaService marca;
    private final PaymentClient payment;
    private final EmailSender sender;
    private final PropiedadesNotificacion propiedades;
    private final Clock reloj;
    private final Pausa pausa;
    private final TransactionTemplate transaccion;

    public DespachadorCorreos(EnvioCorreoRepository envios, PlantillaRepository plantillas,
            RenderizadorCorreo renderizador, MarcaService marca, PaymentClient payment, EmailSender sender,
            PropiedadesNotificacion propiedades, Clock reloj, Pausa pausa, TransactionTemplate transaccion) {
        this.envios = envios;
        this.plantillas = plantillas;
        this.renderizador = renderizador;
        this.marca = marca;
        this.payment = payment;
        this.sender = sender;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.pausa = pausa;
        this.transaccion = transaccion;
    }

    @Scheduled(fixedDelayString = "${fitstore.notificacion.intervalo-despacho:5s}", initialDelayString = "5s")
    public void programado() {
        try {
            despachar();
        } catch (RuntimeException e) {
            LOG.error("Falló un ciclo del despachador", e);
        }
    }

    /** Un ciclo: hasta {@code ritmo × segundos del ciclo} correos, con la pausa que cumple el ritmo por segundo. */
    public void despachar() {
        long espera = 1000L / propiedades.ritmoPorSegundo();
        transaccion.executeWithoutResult(estado -> {
            List<EnvioCorreo> lote = envios.tomarPendientes(reloj.instant(), propiedades.lotePorCiclo());
            for (int i = 0; i < lote.size(); i++) {
                if (i > 0) {
                    pausa.esperar(espera);
                }
                enviar(lote.get(i));
            }
        });
    }

    private void enviar(EnvioCorreo envio) {
        try {
            CatalogoPlantillas catalogo = CatalogoPlantillas.valueOf(envio.getPlantilla());
            var plantilla = plantillas.findById(envio.getPlantilla())
                    .orElseThrow(() -> new IllegalStateException("La plantilla " + envio.getPlantilla() + " no existe"));
            MarcaVista vigente = marca.actual();
            CorreoRenderizado correo = renderizador.renderizar(catalogo, TextosPlantilla.de(plantilla),
                    envio.getVariables(), vigente);
            AdjuntoCorreo adjunto = null;
            if (envio.getAdjunto() != null) {
                adjunto = new AdjuntoCorreo(envio.getAdjunto() + ".pdf", "application/pdf",
                        payment.descargarBoleta(envio.getAdjunto()));
            }
            sender.enviar(new MensajeCorreo(propiedades.remitente(), vigente.nombre(), envio.getDestinatario(),
                    correo.asunto(), correo.html(), correo.texto(), adjunto));
            registrarExito(envio, correo.asunto());
        } catch (RuntimeException e) {
            registrarFallo(envio, e);
        }
    }

    private void registrarExito(EnvioCorreo envio, String asunto) {
        Instant ahora = reloj.instant();
        envio.setAsunto(asunto.length() <= 255 ? asunto : asunto.substring(0, 255));
        envio.setIntentos(envio.getIntentos() + 1);
        envio.setHistorialIntentos(conIntento(envio, ahora, null));
        envio.setEstado(EstadoEnvio.ENVIADO);
        envio.setEnviadoEn(ahora);
        envio.setProximoIntentoEn(null);
        envio.setUltimoError(null);
        envios.save(envio);
    }

    private void registrarFallo(EnvioCorreo envio, RuntimeException e) {
        Instant ahora = reloj.instant();
        String error = mensaje(e);
        List<Duration> esperas = propiedades.reintentos();
        envio.setIntentos(envio.getIntentos() + 1);
        envio.setHistorialIntentos(conIntento(envio, ahora, error));
        envio.setUltimoError(error);
        if (envio.getIntentos() > esperas.size()) {
            envio.setEstado(EstadoEnvio.FALLIDO);
            envio.setProximoIntentoEn(null);
            LOG.warn("Envío {} a {} FALLIDO tras {} intentos: {}", envio.getId(), envio.getDestinatario(),
                    envio.getIntentos(), error);
        } else {
            envio.setEstado(EstadoEnvio.PENDIENTE);
            envio.setProximoIntentoEn(ahora.plus(esperas.get(envio.getIntentos() - 1)));
            LOG.warn("Envío {} a {} falló (intento {}); se reintenta {}: {}", envio.getId(), envio.getDestinatario(),
                    envio.getIntentos(), envio.getProximoIntentoEn(), error);
        }
        envios.save(envio);
    }

    /** Los intentos se numeran de corrido aunque el admin reenvíe y el contador del ciclo vuelva a cero. */
    private static List<Map<String, Object>> conIntento(EnvioCorreo envio, Instant fecha, String error) {
        List<Map<String, Object>> historial = new ArrayList<>(envio.getHistorialIntentos());
        Map<String, Object> intento = new LinkedHashMap<>();
        intento.put("numero", historial.size() + 1);
        intento.put("fecha", fecha.toString());
        intento.put("error", error);
        historial.add(intento);
        return historial;
    }

    private static String mensaje(RuntimeException e) {
        String texto = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }
}
