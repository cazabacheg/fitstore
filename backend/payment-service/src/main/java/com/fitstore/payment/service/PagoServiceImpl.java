package com.fitstore.payment.service;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.payment.client.ConfiguracionTienda;
import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.dto.IntentoRechazado;
import com.fitstore.payment.dto.ItemPago;
import com.fitstore.payment.dto.PaginaPago;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.ProveedorRegistry;
import com.fitstore.payment.repository.BoletaRepository;
import com.fitstore.payment.repository.IntencionPagoRepository;
import com.fitstore.payment.repository.TransaccionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoServiceImpl implements PagoService {

    private final IntencionPagoRepository intenciones;
    private final TransaccionRepository transacciones;
    private final BoletaRepository boletas;
    private final ProveedorRegistry registro;
    private final ConfiguracionTienda tienda;
    private final PropiedadesPagos propiedades;

    public PagoServiceImpl(IntencionPagoRepository intenciones, TransaccionRepository transacciones,
            BoletaRepository boletas, ProveedorRegistry registro, ConfiguracionTienda tienda,
            PropiedadesPagos propiedades) {
        this.intenciones = intenciones;
        this.transacciones = transacciones;
        this.boletas = boletas;
        this.registro = registro;
        this.tienda = tienda;
        this.propiedades = propiedades;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaPago obtenerPagina(String codigoPedido, UUID clienteId) {
        IntencionPago intencion = intenciones.findByCodigoPedido(codigoPedido)
                .orElseThrow(() -> new FitstoreException(HttpStatus.NOT_FOUND, "PREPARANDO_PAGO", "Preparando tu pago…"));
        if (!intencion.getClienteId().equals(clienteId)) {
            throw new RecursoNoEncontradoException("No encontramos ese pedido");
        }
        List<Transaccion> intentos = transacciones.findByIntencionIdOrderByCreadoEnAsc(intencion.getId());
        List<IntentoRechazado> rechazados = intentos.stream()
                .filter(t -> t.getEstado() == EstadoTransaccion.RECHAZADA)
                .map(t -> new IntentoRechazado(t.getCreadoEn(), t.getProveedor(), t.getMedio(), t.getMensaje()))
                .toList();
        boolean procesando = intentos.stream().anyMatch(t -> t.getEstado() == EstadoTransaccion.PROCESANDO);
        return new PaginaPago(intencion.getCodigoPedido(), intencion.getEstado(), intencion.getMonto(),
                intencion.getItems().stream().map(PagoServiceImpl::item).toList(), intencion.getCostoEnvio(),
                intencion.getExpiraEn(),
                registro.disponibles(tienda.metodosPago().orElse(null), intencion.getMonto()),
                propiedades.yape().topeDiario(), rechazados, procesando,
                boletas.findByIntencionId(intencion.getId()).isPresent(), propiedades.simulado().habilitado());
    }

    private static ItemPago item(Map<String, Object> i) {
        return new ItemPago(texto(i.get("producto")), texto(i.get("talla")), texto(i.get("color")),
                i.get("cantidad") instanceof Number n ? n.intValue() : 0, monto(i.get("total")));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static BigDecimal monto(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }
}
