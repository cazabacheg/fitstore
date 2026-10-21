package com.fitstore.payment.provider;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.util.List;

/**
 * Estrategia de una pasarela de pago (micro 10.2). Cada adaptador traduce los modelos y códigos de su pasarela a
 * {@link ResultadoCargo} (con mensaje en español) y convierte los montos; nada fuera de {@code provider/} conoce las
 * APIs externas. Los reembolsos son extensión de alcance y no forman parte de esta interfaz todavía.
 */
public interface PaymentProvider {

    Proveedor proveedor();

    /** Un proveedor sin credenciales no se ofrece en la página de pago (micro 3.3). */
    boolean tieneCredenciales();

    /** Clave pública para la librería del navegador; {@code null} si el proveedor no la necesita (simulado). */
    String clavePublica();

    /** Medios de pago que ofrece este proveedor. */
    List<Medio> medios();

    /**
     * Crea el cargo por el monto de la solicitud. No se reintenta: si la pasarela no responde lanza
     * {@link PasarelaSinRespuestaException} y el estado se resuelve por webhook o por el verificador.
     */
    ResultadoCargo crearCargo(SolicitudCargo solicitud);

    /** Consulta a la pasarela el estado actual de un cargo por su id. */
    ResultadoCargo consultarEstado(String idProveedor);

    /**
     * Busca un cargo por la referencia que se le envió al crearlo (cargos que quedaron sin respuesta y de los que no
     * se conoce el id). Devuelve {@code null} si la pasarela no tiene rastro del cargo o no soporta la búsqueda.
     */
    default ResultadoCargo consultarPorReferencia(String referencia) {
        return null;
    }

    /** Valida la autenticidad de un webhook y extrae el evento. @throws FirmaInvalidaException si no es auténtico */
    default EventoProveedor verificarWebhook(WebhookEntrante webhook) {
        throw new UnsupportedOperationException("El proveedor " + proveedor() + " no recibe webhooks");
    }
}
