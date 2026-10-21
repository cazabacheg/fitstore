package com.fitstore.payment.service;

import com.fitstore.payment.event.PedidoCreado;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.repository.IntencionPagoRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntencionPagoServiceImpl implements IntencionPagoService {

    private static final Logger LOG = LoggerFactory.getLogger(IntencionPagoServiceImpl.class);

    private final IntencionPagoRepository intenciones;

    public IntencionPagoServiceImpl(IntencionPagoRepository intenciones) {
        this.intenciones = intenciones;
    }

    @Override
    @Transactional
    public void crear(PedidoCreado pedido) {
        if (intenciones.findByPedidoId(pedido.pedidoId()).isPresent()) {
            LOG.info("Ya existe la intención del pedido {}; se ignora el evento repetido", pedido.codigo());
            return;
        }
        IntencionPago intencion = new IntencionPago();
        intencion.setPedidoId(pedido.pedidoId());
        intencion.setCodigoPedido(pedido.codigo());
        intencion.setClienteId(pedido.clienteId());
        intencion.setMonto(pedido.total());
        intencion.setEstado(EstadoIntencion.PENDIENTE);
        intencion.setExpiraEn(pedido.expiraEn());
        intencion.setItems(pedido.items().stream().map(IntencionPagoServiceImpl::item).toList());
        intencion.setCliente(cliente(pedido.destinatario()));
        intencion.setSubtotal(pedido.subtotal());
        intencion.setDescuentoTotal(pedido.descuentoTotal());
        intencion.setCostoEnvio(pedido.costoEnvio());
        intenciones.saveAndFlush(intencion);
    }

    @Override
    @Transactional
    public void anular(UUID pedidoId) {
        intenciones.findByPedidoId(pedidoId)
                .filter(i -> i.getEstado() == EstadoIntencion.PENDIENTE)
                .ifPresent(i -> i.setEstado(EstadoIntencion.ANULADA));
    }

    private static Map<String, Object> item(PedidoCreado.ItemPedido i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sku", i.sku());
        m.put("producto", i.producto());
        m.put("talla", i.talla());
        m.put("color", i.color());
        m.put("cantidad", i.cantidad());
        m.put("precioUnitario", i.precioUnitario());
        m.put("descuentoPorcentaje", i.descuentoPorcentaje());
        m.put("descuentoMonto", i.descuentoMonto());
        m.put("total", i.total());
        return m;
    }

    private static Map<String, Object> cliente(PedidoCreado.Destinatario d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombre", (d.nombres() + " " + d.apellidos()).trim());
        m.put("dni", d.dni());
        m.put("email", d.email());
        return m;
    }
}
