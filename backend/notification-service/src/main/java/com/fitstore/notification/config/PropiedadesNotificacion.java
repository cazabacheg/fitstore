package com.fitstore.notification.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de notificaciones ({@code fitstore.notificacion}, micro 10.5).
 *
 * @param remitente                  dirección verificada en SES (en dev, cualquiera: Mailpit no valida)
 * @param ritmoPorSegundo            máximo de correos por segundo (SPEC-7 N6: 1 en cloud, 10 en dev)
 * @param reintentos                 espera tras cada fallo: 1, 5, 15 y 60 minutos; superado el último, queda FALLIDO
 * @param confirmacionSinBoletaTras  respaldo: se confirma el pedido sin adjunto si la boleta no llegó en este tiempo
 * @param retencion                  días que se guarda el registro (limpieza de la extensión de alcance)
 * @param intervaloDespacho          cada cuánto corre el despachador
 * @param programacionHabilitada     {@code false} en las pruebas, que llaman a los procesos a mano
 * @param paymentUrl                 base de payment, para descargar la boleta de {@code /internal/v1/boletas}
 * @param settingsUrl                base de settings, solo para pedir la marca al arrancar sin copia
 * @param enlaces                    URLs públicas de la tienda y del backoffice, para los botones de los correos
 */
@ConfigurationProperties(prefix = "fitstore.notificacion")
public record PropiedadesNotificacion(String remitente, Integer ritmoPorSegundo, List<Duration> reintentos,
        Duration confirmacionSinBoletaTras, Duration retencion, Duration intervaloDespacho,
        Boolean programacionHabilitada, String paymentUrl, String settingsUrl, Enlaces enlaces) {

    public PropiedadesNotificacion {
        remitente = remitente == null || remitente.isBlank() ? "no-responder@fitstore.local" : remitente;
        ritmoPorSegundo = ritmoPorSegundo == null || ritmoPorSegundo < 1 ? 1 : ritmoPorSegundo;
        reintentos = reintentos == null || reintentos.isEmpty()
                ? List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(60))
                : List.copyOf(reintentos);
        confirmacionSinBoletaTras = confirmacionSinBoletaTras == null ? Duration.ofMinutes(10) : confirmacionSinBoletaTras;
        retencion = retencion == null ? Duration.ofDays(180) : retencion;
        intervaloDespacho = intervaloDespacho == null ? Duration.ofSeconds(5) : intervaloDespacho;
        programacionHabilitada = programacionHabilitada == null || programacionHabilitada;
        enlaces = enlaces == null ? new Enlaces(null, null) : enlaces;
    }

    /** URLs base sin la barra final. */
    public record Enlaces(String storefront, String backoffice) {

        public Enlaces {
            storefront = sinBarraFinal(storefront);
            backoffice = sinBarraFinal(backoffice);
        }

        private static String sinBarraFinal(String url) {
            if (url == null) {
                return "";
            }
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
    }

    /** Correos por ciclo del despachador: el ritmo por segundo por los segundos que dura el ciclo. */
    public int lotePorCiclo() {
        return (int) Math.max(1, ritmoPorSegundo * intervaloDespacho.toSeconds());
    }
}
