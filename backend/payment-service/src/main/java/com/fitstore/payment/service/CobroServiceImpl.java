package com.fitstore.payment.service;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.payment.client.ConfiguracionTienda;
import com.fitstore.payment.dto.CargoRequest;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.CircuitoProveedores;
import com.fitstore.payment.provider.PasarelaSinRespuestaException;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ProveedorRegistry;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.provider.SolicitudCargo;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobro con bloqueo: la intención se bloquea con {@code SELECT … FOR UPDATE}, así dos cobros simultáneos se serializan y el
 * segundo encuentra la intención ya pagada (409). Junto con el índice único de transacciones aprobadas, nunca hay doble
 * cobro. La llamada a la pasarela no se reintenta: si no responde, la transacción queda PROCESANDO y la resuelven el
 * webhook o el verificador.
 */
@Service
public class CobroServiceImpl implements CobroService {

    private static final Logger LOG = LoggerFactory.getLogger(CobroServiceImpl.class);

    private final IntencionPagoRepository intenciones;
    private final TransaccionRepository transacciones;
    private final ProveedorRegistry registro;
    private final CircuitoProveedores circuitos;
    private final ConfiguracionTienda tienda;
    private final LiquidadorPagos liquidador;

    public CobroServiceImpl(IntencionPagoRepository intenciones, TransaccionRepository transacciones,
            ProveedorRegistry registro, CircuitoProveedores circuitos, ConfiguracionTienda tienda,
            LiquidadorPagos liquidador) {
        this.intenciones = intenciones;
        this.transacciones = transacciones;
        this.registro = registro;
        this.circuitos = circuitos;
        this.tienda = tienda;
        this.liquidador = liquidador;
    }

    @Override
    @Transactional
    public ResultadoCobro cobrar(String codigoPedido, UUID clienteId, CargoRequest solicitud, String idempotencyKey) {
        IntencionPago intencion = intenciones.bloquearPorCodigoPedido(codigoPedido)
                .filter(i -> i.getClienteId().equals(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese pedido"));

        // Misma clave: se devuelve el resultado del intento original, sin un nuevo cargo (CA-5-17)
        Optional<Transaccion> previa = transacciones.findByIntencionIdAndIdempotencyKey(intencion.getId(), idempotencyKey);
        if (previa.isPresent()) {
            return new ResultadoCobro(previa.get(), intencion);
        }

        validarIntencion(intencion);
        if (transacciones.existsByIntencionIdAndEstado(intencion.getId(), EstadoTransaccion.PROCESANDO)) {
            // Un cargo anterior sin confirmar podría estar cobrado: no se acepta otro hasta saber qué pasó con él
            throw new FitstoreException(HttpStatus.CONFLICT, "PAGO_EN_CURSO",
                    "Estamos confirmando tu pago anterior; espera su resultado antes de intentar de nuevo");
        }
        List<String> metodosTienda = tienda.metodosPago().orElse(null);
        PaymentProvider proveedor = registro.validar(solicitud.proveedor(), solicitud.medio(), intencion.getMonto(), metodosTienda);

        Transaccion transaccion = new Transaccion();
        transaccion.setIntencionId(intencion.getId());
        transaccion.setProveedor(solicitud.proveedor());
        transaccion.setMedio(solicitud.medio());
        transaccion.setEstado(EstadoTransaccion.PROCESANDO);
        transaccion.setMonto(intencion.getMonto());
        transaccion.setIdempotencyKey(idempotencyKey);
        transaccion = transacciones.saveAndFlush(transaccion);

        SolicitudCargo cargo = new SolicitudCargo(intencion.getId(), transaccion.getId().toString(),
                intencion.getCodigoPedido(), intencion.getMonto(), solicitud.token(), solicitud.medio(),
                emailDe(solicitud, intencion), idempotencyKey, solicitud.datosAdicionales());
        try {
            ResultadoCargo resultado = circuitos.ejecutar(solicitud.proveedor(), () -> proveedor.crearCargo(cargo));
            liquidador.aplicar(intencion, transaccion, resultado);
        } catch (PasarelaSinRespuestaException e) {
            // Sin reintento: podría cobrarse dos veces. Queda PROCESANDO para el webhook o el verificador (CA-5-18)
            LOG.warn("La pasarela {} no respondió al cobrar el pedido {}; queda en curso",
                    solicitud.proveedor(), intencion.getCodigoPedido());
            transaccion.setMensaje("La pasarela no respondió a tiempo");
        }
        return new ResultadoCobro(transaccion, intencion);
    }

    private static void validarIntencion(IntencionPago intencion) {
        if (intencion.getEstado() == EstadoIntencion.ANULADA) {
            throw new FitstoreException(HttpStatus.CONFLICT, "PAGO_ANULADO", "Este pedido fue cancelado");
        }
        if (intencion.getEstado() != EstadoIntencion.PENDIENTE) {
            throw new FitstoreException(HttpStatus.CONFLICT, "YA_PAGADO", "Este pedido ya fue pagado");
        }
        if (!intencion.getExpiraEn().isAfter(Instant.now())) {
            throw new FitstoreException(HttpStatus.CONFLICT, "PAGO_VENCIDO", "El plazo para pagar venció");
        }
    }

    private static String emailDe(CargoRequest solicitud, IntencionPago intencion) {
        if (solicitud.email() != null && !solicitud.email().isBlank()) {
            return solicitud.email();
        }
        Object email = intencion.getCliente().get("email");
        return email == null ? null : email.toString();
    }
}
