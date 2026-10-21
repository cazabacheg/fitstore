package com.fitstore.payment.messaging;

import com.fitstore.payment.service.BoletaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emite de forma asíncrona las boletas de los pagos confirmados (T6). Si Garage o settings fallan, el pago sigue
 * confirmado y la boleta se reintenta en la siguiente pasada.
 */
@Component
@ConditionalOnProperty(prefix = "fitstore.pagos.tareas", name = "habilitadas", matchIfMissing = true)
public class ProcesadorBoletas {

    private final BoletaService boletas;

    public ProcesadorBoletas(BoletaService boletas) {
        this.boletas = boletas;
    }

    @Scheduled(fixedDelayString = "${fitstore.pagos.boleta.cada:20s}", initialDelayString = "${fitstore.pagos.boleta.cada:20s}")
    public void emitir() {
        boletas.emitirPendientes();
    }
}
