package com.fitstore.notification.envio;

import com.fitstore.notification.config.PropiedadesNotificacion;
import com.fitstore.notification.model.PedidoResumen;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Respaldo de la boleta (micro 4.3): si 10 minutos después de {@code PedidoPagado} no llegó {@code BoletaEmitida}, se
 * envía la confirmación sin adjunto para que el cliente siempre la reciba (CA-7-14).
 */
@Component
public class TemporizadorConfirmacion {

    private static final Logger LOG = LoggerFactory.getLogger(TemporizadorConfirmacion.class);

    private final PedidoResumenRepository pedidos;
    private final ConfirmacionPedido confirmacion;
    private final PropiedadesNotificacion propiedades;
    private final Clock reloj;

    public TemporizadorConfirmacion(PedidoResumenRepository pedidos, ConfirmacionPedido confirmacion,
            PropiedadesNotificacion propiedades, Clock reloj) {
        this.pedidos = pedidos;
        this.confirmacion = confirmacion;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${fitstore.notificacion.intervalo-respaldo:30s}", initialDelayString = "30s")
    public void programado() {
        try {
            revisar();
        } catch (RuntimeException e) {
            LOG.error("Falló la revisión del respaldo de la boleta", e);
        }
    }

    @Transactional
    public void revisar() {
        Instant limite = reloj.instant().minus(propiedades.confirmacionSinBoletaTras());
        for (PedidoResumen pedido : pedidos.pagadosSinConfirmar(limite)) {
            LOG.info("Pedido {}: sin boleta tras {}; se confirma sin adjunto", pedido.getCodigo(),
                    propiedades.confirmacionSinBoletaTras());
            confirmacion.confirmar(null, pedido, null);
        }
    }
}
