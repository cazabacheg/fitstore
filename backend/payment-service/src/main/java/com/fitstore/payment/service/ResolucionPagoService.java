package com.fitstore.payment.service;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica lo que la pasarela informó (por webhook o por el verificador) a una transacción que estaba en curso. Bloquea la
 * intención igual que el cobro, así un webhook y el verificador a la vez no se pisan, y es idempotente: lo que ya no está
 * PROCESANDO no se toca.
 */
@Service
public class ResolucionPagoService {

    private static final Logger LOG = LoggerFactory.getLogger(ResolucionPagoService.class);

    private final TransaccionRepository transacciones;
    private final IntencionPagoRepository intenciones;
    private final LiquidadorPagos liquidador;
    private final EntityManager em;

    public ResolucionPagoService(TransaccionRepository transacciones, IntencionPagoRepository intenciones,
            LiquidadorPagos liquidador, EntityManager em) {
        this.transacciones = transacciones;
        this.intenciones = intenciones;
        this.liquidador = liquidador;
        this.em = em;
    }

    /** @return {@code true} si la transacción dejó de estar en curso */
    @Transactional
    public boolean resolver(UUID transaccionId, ResultadoCargo resultado) {
        Optional<Transaccion> encontrada = transacciones.findById(transaccionId);
        if (encontrada.isEmpty() || encontrada.get().getEstado() != EstadoTransaccion.PROCESANDO) {
            return false;
        }
        Transaccion transaccion = encontrada.get();
        IntencionPago intencion = intenciones.bloquearPorId(transaccion.getIntencionId()).orElseThrow();
        em.refresh(transaccion); // otro proceso pudo resolverla mientras se esperaba el bloqueo
        liquidador.aplicar(intencion, transaccion, resultado);
        return transaccion.getEstado() != EstadoTransaccion.PROCESANDO;
    }

    /**
     * Resuelve el cargo que la pasarela identifica con {@code idPago}: se ubica la transacción por ese id o, si todavía no lo
     * tenía (el cargo quedó sin respuesta), por la referencia que se envió al crearlo.
     *
     * @return {@code true} si alguna transacción dejó de estar en curso
     */
    @Transactional
    public boolean resolverPorPago(Proveedor proveedor, String idPago, ResultadoCargo resultado) {
        Optional<Transaccion> transaccion = transacciones.findByProveedorAndIdProveedor(proveedor, idPago);
        if (transaccion.isEmpty() && resultado.respuesta() != null
                && resultado.respuesta().get("external_reference") instanceof String referencia) {
            transaccion = ubicarPorReferencia(referencia);
        }
        if (transaccion.isEmpty()) {
            LOG.info("El aviso del pago {} de {} no corresponde a ninguna transacción conocida", idPago, proveedor);
            return false;
        }
        return resolver(transaccion.get().getId(), resultado.idProveedor() == null ? conId(resultado, idPago) : resultado);
    }

    /** Pasa a ERROR un cargo que la pasarela nunca registró (sin id y sin rastro tras mucho tiempo). */
    @Transactional
    public void marcarError(UUID transaccionId, String mensaje) {
        transacciones.findById(transaccionId)
                .filter(t -> t.getEstado() == EstadoTransaccion.PROCESANDO)
                .ifPresent(t -> {
                    intenciones.bloquearPorId(t.getIntencionId()).orElseThrow();
                    em.refresh(t);
                    if (t.getEstado() == EstadoTransaccion.PROCESANDO) {
                        t.setEstado(EstadoTransaccion.ERROR);
                        t.setMensaje(mensaje);
                    }
                });
    }

    private Optional<Transaccion> ubicarPorReferencia(String referencia) {
        try {
            return transacciones.findById(UUID.fromString(referencia));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static ResultadoCargo conId(ResultadoCargo r, String idPago) {
        return new ResultadoCargo(r.estado(), idPago, r.codigo(), r.mensajeLegible(), r.descripcionMedio(), r.respuesta());
    }
}
