package com.fitstore.order.pedidos.messaging;

import com.fitstore.order.pedidos.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Planificador que expira cada minuto los pedidos PENDIENTE_PAGO vencidos (micro 4.4). */
@Component
public class ExpiradorPedidos {

    private static final Logger LOG = LoggerFactory.getLogger(ExpiradorPedidos.class);

    private final PedidoService pedidos;

    public ExpiradorPedidos(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @Scheduled(cron = "${fitstore.order.expirador-cron}")
    public void expirarVencidos() {
        int expirados = pedidos.expirarVencidos();
        if (expirados > 0) {
            LOG.info("Se expiraron {} pedidos sin pago", expirados);
        }
    }
}
