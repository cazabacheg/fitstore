package com.fitstore.payment.service;

import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.CircuitoProveedores;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ProveedorRegistry;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.repository.TransaccionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Verificador de pagos en curso (micro 4.4): consulta a la pasarela las transacciones PROCESANDO con más de
 * {@code fitstore.pagos.verificador.antiguedad} (2 min) y las aprueba o rechaza. Cubre los webhooks que no llegan, por
 * ejemplo en local. Un cargo del que ni siquiera se conoce el id y que la pasarela no tiene por referencia pasa a ERROR
 * tras cinco veces esa antigüedad. La consulta a la pasarela va fuera de toda transacción de base de datos.
 */
@Service
public class VerificadorPagosService {

    private static final Logger LOG = LoggerFactory.getLogger(VerificadorPagosService.class);
    private static final int VECES_ANTIGUEDAD_SIN_RASTRO = 5;

    private final TransaccionRepository transacciones;
    private final ProveedorRegistry registro;
    private final CircuitoProveedores circuitos;
    private final ResolucionPagoService resolucion;
    private final PropiedadesPagos propiedades;

    public VerificadorPagosService(TransaccionRepository transacciones, ProveedorRegistry registro,
            CircuitoProveedores circuitos, ResolucionPagoService resolucion, PropiedadesPagos propiedades) {
        this.transacciones = transacciones;
        this.registro = registro;
        this.circuitos = circuitos;
        this.resolucion = resolucion;
        this.propiedades = propiedades;
    }

    /** @return cuántas transacciones dejaron de estar en curso en esta pasada */
    public int verificar() {
        Instant limite = Instant.now().minus(propiedades.verificador().antiguedad());
        List<Transaccion> enCurso = transacciones.findByEstadoAndActualizadoEnBefore(EstadoTransaccion.PROCESANDO, limite);
        int resueltas = 0;
        for (Transaccion transaccion : enCurso) {
            try {
                if (verificarUna(transaccion)) {
                    resueltas++;
                }
            } catch (RuntimeException e) {
                LOG.warn("No se pudo verificar la transacción {}: {}", transaccion.getId(), e.getMessage());
            }
        }
        return resueltas;
    }

    private boolean verificarUna(Transaccion transaccion) {
        Optional<PaymentProvider> proveedor = registro.buscar(transaccion.getProveedor());
        if (proveedor.isEmpty()) {
            return false;
        }
        PaymentProvider p = proveedor.get();
        ResultadoCargo resultado;
        if (transaccion.getIdProveedor() != null) {
            resultado = circuitos.ejecutar(transaccion.getProveedor(), () -> p.consultarEstado(transaccion.getIdProveedor()));
        } else {
            resultado = circuitos.ejecutar(transaccion.getProveedor(),
                    () -> p.consultarPorReferencia(transaccion.getId().toString()));
            if (resultado == null) {
                Instant sinRastroDesde = Instant.now()
                        .minus(propiedades.verificador().antiguedad().multipliedBy(VECES_ANTIGUEDAD_SIN_RASTRO));
                if (transaccion.getActualizadoEn().isBefore(sinRastroDesde)) {
                    resolucion.marcarError(transaccion.getId(), "La pasarela no registró el cobro");
                    return true;
                }
                return false;
            }
        }
        return resolucion.resolver(transaccion.getId(), resultado);
    }
}
