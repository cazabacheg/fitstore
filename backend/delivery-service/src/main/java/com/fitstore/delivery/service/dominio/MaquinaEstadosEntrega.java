package com.fitstore.delivery.service.dominio;

import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.model.AccionEntrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados de la entrega (micro 5): una tabla de transiciones para el delivery local y otra para la agencia.
 * Es pura: no toca la base ni el reloj. La devolución a tienda (estado {@code DEVUELTA}) es extensión de alcance y
 * por eso no tiene transiciones.
 */
public final class MaquinaEstadosEntrega {

    /** Datos que acompañan un avance y que algunas transiciones exigen. */
    public record DatosAvance(String observacion, String codigoSeguimiento) {
    }

    private static final Map<EstadoEntrega, Set<EstadoEntrega>> LOCAL = Map.of(
            EstadoEntrega.PENDIENTE_ASIGNACION, Set.of(EstadoEntrega.ASIGNADA),
            EstadoEntrega.ASIGNADA, Set.of(EstadoEntrega.ASIGNADA, EstadoEntrega.EN_RUTA),
            EstadoEntrega.EN_RUTA, Set.of(EstadoEntrega.ENTREGADA, EstadoEntrega.FALLIDA),
            EstadoEntrega.FALLIDA, Set.of(EstadoEntrega.ASIGNADA));

    private static final Map<EstadoEntrega, Set<EstadoEntrega>> AGENCIA = Map.of(
            EstadoEntrega.PENDIENTE_ENVIO, Set.of(EstadoEntrega.ENVIADO_AGENCIA),
            EstadoEntrega.ENVIADO_AGENCIA, Set.of(EstadoEntrega.DISPONIBLE_EN_AGENCIA),
            EstadoEntrega.DISPONIBLE_EN_AGENCIA, Set.of(EstadoEntrega.ENTREGADA));

    private static final Map<EstadoEntrega, List<AccionEntrega>> ACCIONES = Map.of(
            EstadoEntrega.PENDIENTE_ASIGNACION, List.of(AccionEntrega.ASIGNAR),
            EstadoEntrega.ASIGNADA, List.of(AccionEntrega.REPROGRAMAR, AccionEntrega.EN_RUTA),
            EstadoEntrega.EN_RUTA, List.of(AccionEntrega.ENTREGADA, AccionEntrega.FALLIDA),
            EstadoEntrega.FALLIDA, List.of(AccionEntrega.REPROGRAMAR),
            EstadoEntrega.PENDIENTE_ENVIO, List.of(AccionEntrega.ENVIADA_AGENCIA),
            EstadoEntrega.ENVIADO_AGENCIA, List.of(AccionEntrega.DISPONIBLE_AGENCIA),
            EstadoEntrega.DISPONIBLE_EN_AGENCIA, List.of(AccionEntrega.ENTREGADA));

    /** El paso que se muestra como botón en la fila del listado (E1). */
    private static final Map<EstadoEntrega, AccionEntrega> SIGUIENTE = Map.of(
            EstadoEntrega.PENDIENTE_ASIGNACION, AccionEntrega.ASIGNAR,
            EstadoEntrega.ASIGNADA, AccionEntrega.EN_RUTA,
            EstadoEntrega.EN_RUTA, AccionEntrega.ENTREGADA,
            EstadoEntrega.FALLIDA, AccionEntrega.REPROGRAMAR,
            EstadoEntrega.PENDIENTE_ENVIO, AccionEntrega.ENVIADA_AGENCIA,
            EstadoEntrega.ENVIADO_AGENCIA, AccionEntrega.DISPONIBLE_AGENCIA,
            EstadoEntrega.DISPONIBLE_EN_AGENCIA, AccionEntrega.ENTREGADA);

    private MaquinaEstadosEntrega() {
    }

    public static EstadoEntrega estadoInicial(Modalidad modalidad) {
        return modalidad == Modalidad.DELIVERY_LOCAL ? EstadoEntrega.PENDIENTE_ASIGNACION : EstadoEntrega.PENDIENTE_ENVIO;
    }

    public static boolean permitida(Modalidad modalidad, EstadoEntrega desde, EstadoEntrega hacia) {
        Map<EstadoEntrega, Set<EstadoEntrega>> tabla = modalidad == Modalidad.DELIVERY_LOCAL ? LOCAL : AGENCIA;
        return tabla.getOrDefault(desde, Set.of()).contains(hacia);
    }

    /**
     * Comprueba la transición y, si es válida, los datos obligatorios del avance. El estado se revisa primero (409) y
     * después los datos (400): la observación en fallida y el código de seguimiento al enviar por agencia.
     */
    public static void validar(Modalidad modalidad, EstadoEntrega desde, EstadoEntrega hacia, DatosAvance datos) {
        if (!permitida(modalidad, desde, hacia)) {
            throw new TransicionNoPermitidaException(desde);
        }
        if (hacia == EstadoEntrega.FALLIDA && vacio(datos.observacion())) {
            throw new SolicitudInvalidaException("observacion", "Cuéntanos por qué no se pudo entregar");
        }
        if (hacia == EstadoEntrega.ENVIADO_AGENCIA && vacio(datos.codigoSeguimiento())) {
            throw new SolicitudInvalidaException("codigoSeguimiento", "Ingresa el código de seguimiento");
        }
    }

    public static List<AccionEntrega> accionesPermitidas(Modalidad modalidad, EstadoEntrega estado) {
        return ACCIONES.getOrDefault(estado, List.of()).stream()
                .filter(a -> modalidad == Modalidad.DELIVERY_LOCAL ? esLocal(a) : esAgencia(a, estado))
                .toList();
    }

    /** El siguiente paso de la fila, o {@code null} en los estados finales. */
    public static AccionEntrega siguienteAccion(Modalidad modalidad, EstadoEntrega estado) {
        AccionEntrega accion = SIGUIENTE.get(estado);
        return accion != null && accionesPermitidas(modalidad, estado).contains(accion) ? accion : null;
    }

    private static boolean esLocal(AccionEntrega a) {
        return a != AccionEntrega.ENVIADA_AGENCIA && a != AccionEntrega.DISPONIBLE_AGENCIA;
    }

    private static boolean esAgencia(AccionEntrega a, EstadoEntrega estado) {
        return a == AccionEntrega.ENVIADA_AGENCIA || a == AccionEntrega.DISPONIBLE_AGENCIA
                || (a == AccionEntrega.ENTREGADA && estado == EstadoEntrega.DISPONIBLE_EN_AGENCIA);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
