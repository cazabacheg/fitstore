package com.fitstore.payment.service;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.WebhookRecibido;
import com.fitstore.payment.provider.CircuitoProveedores;
import com.fitstore.payment.provider.EventoProveedor;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ProveedorRegistry;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.provider.WebhookEntrante;
import com.fitstore.payment.repository.WebhookRecibidoRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Orden del procesamiento (micro 4.4): firma → guardado (si el id ya está procesado se ignora) → consulta del estado a la
 * API de la pasarela → resolución idempotente. Responde siempre 200 a un aviso auténtico: si la consulta falla, el
 * aviso queda sin procesar y el verificador resuelve el pago. No hay transacción alrededor de la llamada externa.
 */
@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private final WebhookRecibidoRepository webhooks;
    private final ProveedorRegistry registro;
    private final CircuitoProveedores circuitos;
    private final ResolucionPagoService resolucion;

    public WebhookServiceImpl(WebhookRecibidoRepository webhooks, ProveedorRegistry registro,
            CircuitoProveedores circuitos, ResolucionPagoService resolucion) {
        this.webhooks = webhooks;
        this.registro = registro;
        this.circuitos = circuitos;
        this.resolucion = resolucion;
    }

    @Override
    public void recibirMercadoPago(WebhookEntrante webhook) {
        PaymentProvider proveedor = registro.buscar(Proveedor.MERCADO_PAGO).orElseThrow(() -> new FitstoreException(
                HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Mercado Pago no está configurado"));
        EventoProveedor evento = proveedor.verificarWebhook(webhook);

        Optional<WebhookRecibido> existente = webhooks.findByProveedorAndEventoIdProveedor(Proveedor.MERCADO_PAGO, evento.eventoId());
        WebhookRecibido guardado;
        if (existente.isPresent()) {
            if (existente.get().getProcesadoEn() != null) {
                LOG.info("Webhook {} de Mercado Pago repetido; se ignora", evento.eventoId());
                return;
            }
            guardado = existente.get();
        } else {
            WebhookRecibido nuevo = new WebhookRecibido();
            nuevo.setProveedor(Proveedor.MERCADO_PAGO);
            nuevo.setEventoIdProveedor(evento.eventoId());
            nuevo.setPayload(webhook.cuerpo());
            try {
                guardado = webhooks.saveAndFlush(nuevo);
            } catch (DataIntegrityViolationException e) {
                LOG.info("Webhook {} de Mercado Pago recibido a la vez; lo procesa la otra petición", evento.eventoId());
                return;
            }
        }

        if (evento.idPago() != null) {
            try {
                ResultadoCargo estado = circuitos.ejecutar(Proveedor.MERCADO_PAGO,
                        () -> proveedor.consultarEstado(evento.idPago()));
                resolucion.resolverPorPago(Proveedor.MERCADO_PAGO, evento.idPago(), estado);
            } catch (RuntimeException e) {
                LOG.warn("No se pudo confirmar el pago {} del webhook {}; lo resolverá el verificador: {}",
                        evento.idPago(), evento.eventoId(), e.getMessage());
                return;
            }
        }
        guardado.setProcesadoEn(Instant.now());
        webhooks.save(guardado);
    }
}
