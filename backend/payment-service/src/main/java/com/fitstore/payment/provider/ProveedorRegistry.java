package com.fitstore.payment.provider;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.dto.MedioDisponible;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Registro de proveedores (micro 10.2): se arma con todos los beans {@link PaymentProvider} (el simulado solo existe si
 * está habilitado) y decide qué medios se ofrecen (P7): los habilitados en la tienda que además tienen credenciales,
 * con su disponibilidad (circuito, tope de Yape).
 */
@Component
public class ProveedorRegistry {

    private final Map<Proveedor, PaymentProvider> proveedores = new EnumMap<>(Proveedor.class);
    private final CircuitoProveedores circuitos;
    private final PropiedadesPagos propiedades;

    public ProveedorRegistry(List<PaymentProvider> lista, CircuitoProveedores circuitos, PropiedadesPagos propiedades) {
        lista.forEach(p -> proveedores.put(p.proveedor(), p));
        this.circuitos = circuitos;
        this.propiedades = propiedades;
    }

    public Optional<PaymentProvider> buscar(Proveedor proveedor) {
        return Optional.ofNullable(proveedores.get(proveedor));
    }

    /**
     * Medios que se muestran en la página de pago.
     *
     * @param metodosTienda medios habilitados en settings; {@code null} si settings no respondió (entonces valen todos
     *                      los que tienen credenciales)
     */
    public List<MedioDisponible> disponibles(List<String> metodosTienda, BigDecimal monto) {
        List<MedioDisponible> medios = new ArrayList<>();
        for (PaymentProvider p : proveedores.values()) {
            if (!p.tieneCredenciales()) {
                continue;
            }
            for (Medio medio : p.medios()) {
                if (!habilitadoEnLaTienda(p.proveedor(), medio, metodosTienda)) {
                    continue;
                }
                String motivo = motivoNoDisponible(p.proveedor(), medio, monto);
                medios.add(new MedioDisponible(p.proveedor(), medio, p.clavePublica(), motivo == null, motivo));
            }
        }
        return medios;
    }

    /**
     * Valida que se pueda cobrar con ese proveedor y medio.
     *
     * @throws FitstoreException 400 si no existe, no tiene credenciales, no está habilitado o supera el tope de Yape;
     *                           503 si su circuito está abierto
     */
    public PaymentProvider validar(Proveedor proveedor, Medio medio, BigDecimal monto, List<String> metodosTienda) {
        PaymentProvider p = proveedores.get(proveedor);
        if (p == null || !p.tieneCredenciales() || !p.medios().contains(medio)
                || !habilitadoEnLaTienda(proveedor, medio, metodosTienda)) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "MEDIO_NO_DISPONIBLE",
                    "Ese medio de pago no está disponible");
        }
        if (medio == Medio.YAPE && excedeElTopeDeYape(monto)) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "MEDIO_NO_DISPONIBLE", mensajeTopeYape());
        }
        if (circuitos.estaAbierto(proveedor)) {
            throw new ProveedorNoDisponibleException();
        }
        return p;
    }

    private String motivoNoDisponible(Proveedor proveedor, Medio medio, BigDecimal monto) {
        if (medio == Medio.YAPE && excedeElTopeDeYape(monto)) {
            return mensajeTopeYape();
        }
        if (circuitos.estaAbierto(proveedor)) {
            return ProveedorNoDisponibleException.MENSAJE;
        }
        return null;
    }

    private boolean excedeElTopeDeYape(BigDecimal monto) {
        return monto.compareTo(propiedades.yape().topeDiario()) > 0;
    }

    private String mensajeTopeYape() {
        return "Yape permite hasta S/ " + propiedades.yape().topeDiario().stripTrailingZeros().toPlainString() + " por día";
    }

    private static boolean habilitadoEnLaTienda(Proveedor proveedor, Medio medio, List<String> metodosTienda) {
        if (proveedor == Proveedor.SIMULADO || metodosTienda == null) {
            return true;
        }
        String clave = switch (proveedor) {
            case MERCADO_PAGO -> "MERCADO_PAGO";
            default -> medio == Medio.YAPE ? "YAPE" : "CULQI";
        };
        return metodosTienda.contains(clave);
    }
}
