package com.fitstore.payment.provider.simulado;

import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.PaymentProvider;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.provider.SolicitudCargo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Proveedor simulado de la demo (P2). El formulario genera un token {@code sim_<últimos 4>} y este adaptador decide por
 * esos dígitos: {@code 1111} aprueba, {@code 0002} rechaza por fondos insuficientes, {@code 0119} queda en curso y el
 * verificador lo aprueba en su siguiente pasada; cualquier otro número se aprueba. Solo existe con
 * {@code fitstore.pagos.simulado.habilitado=true} (D7).
 */
@Component
@ConditionalOnProperty(prefix = "fitstore.pagos.simulado", name = "habilitado", havingValue = "true")
public class SimuladoProvider implements PaymentProvider {

    private static final String PREFIJO_ID = "sim-";
    private static final String PREFIJO_ID_EN_CURSO = "sim-pendiente-";

    @Override
    public Proveedor proveedor() {
        return Proveedor.SIMULADO;
    }

    @Override
    public boolean tieneCredenciales() {
        return true;
    }

    @Override
    public String clavePublica() {
        return null;
    }

    @Override
    public List<Medio> medios() {
        return List.of(Medio.TARJETA);
    }

    @Override
    public ResultadoCargo crearCargo(SolicitudCargo solicitud) {
        String ultimos = ultimosCuatro(solicitud.token());
        String descripcion = "Tarjeta de prueba ****" + ultimos;
        Map<String, Object> respuesta = Map.of("simulado", true, "ultimos4", ultimos);
        return switch (ultimos) {
            case "0002" -> new ResultadoCargo(EstadoTransaccion.RECHAZADA, PREFIJO_ID + UUID.randomUUID(), "fondos_insuficientes",
                    "Fondos insuficientes", descripcion, respuesta);
            case "0119" -> new ResultadoCargo(EstadoTransaccion.PROCESANDO, PREFIJO_ID_EN_CURSO + UUID.randomUUID(),
                    "en_proceso", "Estamos confirmando tu pago", descripcion, respuesta);
            default -> new ResultadoCargo(EstadoTransaccion.APROBADA, PREFIJO_ID + UUID.randomUUID(), "aprobado",
                    "¡Pago aprobado!", descripcion, respuesta);
        };
    }

    /** El simulado no tiene servidor: un cargo en curso se aprueba en la siguiente consulta del verificador. */
    @Override
    public ResultadoCargo consultarEstado(String idProveedor) {
        return new ResultadoCargo(EstadoTransaccion.APROBADA, idProveedor, "aprobado", "¡Pago aprobado!", null,
                Map.of("simulado", true));
    }

    private static String ultimosCuatro(String token) {
        String digitos = token.startsWith("sim_") ? token.substring(4) : token;
        return digitos.length() > 4 ? digitos.substring(digitos.length() - 4) : digitos;
    }
}
