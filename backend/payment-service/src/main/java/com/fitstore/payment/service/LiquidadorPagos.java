package com.fitstore.payment.service;

import com.fitstore.common.event.OutboxRegistrador;
import com.fitstore.payment.event.PagoConfirmado;
import com.fitstore.payment.event.PagoRechazado;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.ResultadoCargo;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica el resultado de una pasarela a la transacción y a la intención, y registra el evento en el outbox, todo en la
 * transacción de quien lo llama (cobro, webhook o verificador). Es idempotente: una transacción que ya no está en
 * curso no se toca. La intención debe venir bloqueada.
 */
@Component
public class LiquidadorPagos {

    private static final Logger LOG = LoggerFactory.getLogger(LiquidadorPagos.class);

    private final OutboxRegistrador outbox;

    public LiquidadorPagos(OutboxRegistrador outbox) {
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(IntencionPago intencion, Transaccion transaccion, ResultadoCargo resultado) {
        if (transaccion.getEstado() != EstadoTransaccion.PROCESANDO) {
            return;
        }
        if (resultado.idProveedor() != null) {
            transaccion.setIdProveedor(resultado.idProveedor());
        }
        transaccion.setCodigoRespuesta(resultado.codigo());
        transaccion.setMensaje(resultado.mensajeLegible());
        Map<String, Object> respuesta = new LinkedHashMap<>();
        if (resultado.respuesta() != null) {
            respuesta.putAll(resultado.respuesta());
        }
        if (resultado.descripcionMedio() != null) {
            respuesta.put("descripcionMedio", resultado.descripcionMedio());
        }
        transaccion.setRespuesta(respuesta);

        switch (resultado.estado()) {
            case APROBADA -> aprobar(intencion, transaccion);
            case RECHAZADA -> {
                transaccion.setEstado(EstadoTransaccion.RECHAZADA);
                outbox.registrar("PagoRechazado", intencion.getPedidoId().toString(),
                        new PagoRechazado(intencion.getPedidoId(), transaccion.getProveedor(), resultado.mensajeLegible()));
            }
            default -> transaccion.setEstado(EstadoTransaccion.PROCESANDO);
        }
    }

    private void aprobar(IntencionPago intencion, Transaccion transaccion) {
        if (intencion.getEstado() != EstadoIntencion.PENDIENTE) {
            // La pasarela aprobó un cobro de una intención que ya no se podía pagar: no se confirma el pedido
            transaccion.setEstado(EstadoTransaccion.ERROR);
            transaccion.setMensaje("Cobro aprobado sobre una intención " + intencion.getEstado() + ": requiere revisión");
            LOG.warn("Cobro aprobado sobre la intención {} en estado {}; requiere revisión manual",
                    intencion.getCodigoPedido(), intencion.getEstado());
            return;
        }
        Instant ahora = Instant.now();
        transaccion.setEstado(EstadoTransaccion.APROBADA);
        intencion.setEstado(EstadoIntencion.PAGADA);
        outbox.registrar("PagoConfirmado", intencion.getPedidoId().toString(),
                new PagoConfirmado(intencion.getPedidoId(), intencion.getId(), transaccion.getId(),
                        transaccion.getProveedor(), transaccion.getMedio(), transaccion.getMonto(), ahora));
    }
}
