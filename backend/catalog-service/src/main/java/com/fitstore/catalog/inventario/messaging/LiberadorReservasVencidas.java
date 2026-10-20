package com.fitstore.catalog.inventario.messaging;

import com.fitstore.catalog.inventario.service.ReservaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Red de seguridad del evento {@code PedidoExpirado} (micro 4.7): cada minuto libera las reservas ACTIVA cuya fecha ya
 * pasó, que quedan EXPIRADA. Solo actúa cuando el evento nunca llegó (reserva huérfana o evento perdido); la fecha ya
 * incluye el margen de la reserva, así que si el flujo normal funcionó no encuentra nada.
 */
@Component
public class LiberadorReservasVencidas {

    private final ReservaService reservas;

    public LiberadorReservasVencidas(ReservaService reservas) {
        this.reservas = reservas;
    }

    @Scheduled(cron = "${fitstore.inventario.liberador-reservas-cron:0 * * * * *}")
    public int liberarVencidas() {
        return reservas.liberarVencidas();
    }
}
