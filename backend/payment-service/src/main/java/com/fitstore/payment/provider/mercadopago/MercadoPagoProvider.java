package com.fitstore.payment.provider.mercadopago;

import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.PasarelaSinRespuestaException;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.provider.SolicitudCargo;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Adaptador de Mercado Pago (HU-5-05): el navegador obtiene el token con el Payment Brick y aquí se crea el cargo en
 * {@code POST /v1/payments} con el monto de la intención (decimales) y la misma Idempotency-Key como
 * {@code X-Idempotency-Key}. Usa {@code RestClient} con URL base configurable, así se prueba con WireMock.
 * Nunca se registran el token, las credenciales ni el cuerpo de las respuestas.
 */
public class MercadoPagoProvider implements PaymentProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoProvider.class);

    private final PropiedadesPagos.MercadoPago configuracion;
    private final RestClient cliente;

    public MercadoPagoProvider(PropiedadesPagos.MercadoPago configuracion) {
        this.configuracion = configuracion;
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(configuracion.timeout()).build());
        fabrica.setReadTimeout(configuracion.timeout());
        this.cliente = RestClient.builder().baseUrl(configuracion.url()).requestFactory(fabrica).build();
    }

    @Override
    public Proveedor proveedor() {
        return Proveedor.MERCADO_PAGO;
    }

    @Override
    public boolean tieneCredenciales() {
        return configuracion.tieneCredenciales();
    }

    @Override
    public String clavePublica() {
        return configuracion.publicKey();
    }

    @Override
    public List<Medio> medios() {
        return List.of(Medio.TARJETA);
    }

    @Override
    public ResultadoCargo crearCargo(SolicitudCargo solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("transaction_amount", solicitud.monto());
        cuerpo.put("token", solicitud.token());
        cuerpo.put("description", "Pedido " + solicitud.codigoPedido());
        cuerpo.put("installments", 1);
        copiar(solicitud.datosAdicionales(), cuerpo, "installments", "payment_method_id", "issuer_id");
        if (solicitud.email() != null) {
            cuerpo.put("payer", Map.of("email", solicitud.email()));
        }
        cuerpo.put("external_reference", solicitud.referencia());
        try {
            JsonNode pago = cliente.post().uri("/v1/payments")
                    .header("Authorization", "Bearer " + configuracion.accessToken())
                    .header("X-Idempotency-Key", solicitud.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
            return traducir(pago);
        } catch (HttpClientErrorException e) {
            // La pasarela rechazó la petición (datos inválidos, token vencido): no hubo cargo
            LOG.warn("Mercado Pago rechazó la solicitud de cargo con HTTP {}", e.getStatusCode().value());
            return new ResultadoCargo(EstadoTransaccion.RECHAZADA, null, "http_" + e.getStatusCode().value(),
                    "No pudimos procesar tu pago; revisa los datos e inténtalo de nuevo", null,
                    Map.of("http", e.getStatusCode().value()));
        } catch (HttpServerErrorException | ResourceAccessException e) {
            LOG.warn("Mercado Pago no respondió al crear el cargo: {}", e.getClass().getSimpleName());
            throw new PasarelaSinRespuestaException("Mercado Pago no respondió", e);
        }
    }

    @Override
    public ResultadoCargo consultarEstado(String idProveedor) {
        try {
            JsonNode pago = cliente.get().uri("/v1/payments/{id}", idProveedor)
                    .header("Authorization", "Bearer " + configuracion.accessToken())
                    .retrieve()
                    .body(JsonNode.class);
            return traducir(pago);
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            LOG.warn("No se pudo consultar el pago {} en Mercado Pago: {}", idProveedor, e.getClass().getSimpleName());
            throw new PasarelaSinRespuestaException("No se pudo consultar Mercado Pago", e);
        }
    }

    private static void copiar(Map<String, Object> origen, Map<String, Object> destino, String... claves) {
        if (origen == null) {
            return;
        }
        for (String clave : claves) {
            if (origen.get(clave) != null) {
                destino.put(clave, origen.get(clave));
            }
        }
    }

    /** Traduce la respuesta de Mercado Pago: guarda solo datos no sensibles. */
    static ResultadoCargo traducir(JsonNode pago) {
        String estado = pago.path("status").asText("");
        String detalle = pago.path("status_detail").asText("");
        String id = pago.hasNonNull("id") ? pago.get("id").asText() : null;
        String metodo = pago.path("payment_method_id").asText(null);
        String ultimos = pago.path("card").path("last_four_digits").asText(null);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", id);
        respuesta.put("status", estado);
        respuesta.put("status_detail", detalle);
        respuesta.put("payment_method_id", metodo);
        respuesta.put("payment_type_id", pago.path("payment_type_id").asText(null));
        respuesta.put("last_four_digits", ultimos);
        String descripcion = descripcionMedio(metodo, ultimos);

        return switch (estado) {
            case "approved" -> new ResultadoCargo(EstadoTransaccion.APROBADA, id, detalle, "¡Pago aprobado!", descripcion, respuesta);
            case "rejected", "cancelled", "refunded", "charged_back" ->
                new ResultadoCargo(EstadoTransaccion.RECHAZADA, id, detalle, motivoLegible(detalle), descripcion, respuesta);
            default -> new ResultadoCargo(EstadoTransaccion.PROCESANDO, id, detalle, "Estamos confirmando tu pago", descripcion, respuesta);
        };
    }

    private static String descripcionMedio(String metodo, String ultimos) {
        if (metodo == null) {
            return null;
        }
        String nombre = "account_money".equals(metodo) ? "Saldo Mercado Pago"
                : Character.toUpperCase(metodo.charAt(0)) + metodo.substring(1);
        return ultimos == null ? nombre : nombre + " ****" + ultimos;
    }

    /** Traduce los {@code status_detail} de rechazo a mensajes legibles en español. */
    static String motivoLegible(String detalle) {
        return switch (detalle) {
            case "cc_rejected_insufficient_amount" -> "Fondos insuficientes";
            case "cc_rejected_bad_filled_date" -> "Tarjeta vencida o fecha incorrecta";
            case "cc_rejected_bad_filled_card_number", "cc_rejected_bad_filled_security_code", "cc_rejected_bad_filled_other" ->
                "Revisa los datos de tu tarjeta";
            case "cc_rejected_call_for_authorize" -> "Debes autorizar el pago con tu banco";
            case "cc_rejected_card_disabled" -> "Tu tarjeta está inactiva; actívala con tu banco";
            case "cc_rejected_max_attempts" -> "Superaste el máximo de intentos; prueba con otra tarjeta";
            case "cc_rejected_high_risk" -> "Pago rechazado por seguridad; prueba con otro medio";
            case "cc_rejected_duplicated_payment" -> "Ya hiciste un pago igual hace poco";
            default -> "Tarjeta rechazada por el banco";
        };
    }
}
