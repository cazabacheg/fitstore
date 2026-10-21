package com.fitstore.payment.messaging;

import com.fitstore.payment.service.VerificadorPagosService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el verificador de pagos en curso cada 2 minutos ({@code fitstore.pagos.verificador.cada}). */
@Component
@ConditionalOnProperty(prefix = "fitstore.pagos.tareas", name = "habilitadas", matchIfMissing = true)
public class VerificadorPendientes {

    private final VerificadorPagosService verificador;

    public VerificadorPendientes(VerificadorPagosService verificador) {
        this.verificador = verificador;
    }

    @Scheduled(fixedDelayString = "${fitstore.pagos.verificador.cada:2m}",
            initialDelayString = "${fitstore.pagos.verificador.cada:2m}")
    public void verificar() {
        verificador.verificar();
    }
}
