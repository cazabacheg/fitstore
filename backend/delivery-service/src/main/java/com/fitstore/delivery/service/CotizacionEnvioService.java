package com.fitstore.delivery.service;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.delivery.config.PropiedadesDelivery;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.CotizacionEnvioDto;
import com.fitstore.delivery.dto.OpcionEnvioDto;
import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.repository.AgenciaRepository;
import com.fitstore.delivery.repository.TarifaEnvioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cotización de envíos (RF-DEL-05, micro 4.2). El distrito es obligatorio; la jerarquía distrito, provincia y
 * departamento está en cómo se configuran las tarifas, y se resuelve con una sola consulta.
 */
@Service
@Transactional(readOnly = true)
public class CotizacionEnvioService {

    public static final String MENSAJE_ELIGE_DISTRITO = "Elige el distrito de destino";
    public static final String MENSAJE_SIN_COBERTURA = "No realizamos envíos por esta modalidad a este distrito";

    private final TarifaEnvioRepository tarifas;
    private final AgenciaRepository agencias;
    private final PropiedadesDelivery propiedades;

    public CotizacionEnvioService(TarifaEnvioRepository tarifas, AgenciaRepository agencias,
            PropiedadesDelivery propiedades) {
        this.tarifas = tarifas;
        this.agencias = agencias;
        this.propiedades = propiedades;
    }

    /** Modalidades con tarifa para el distrito; puede ser una lista vacía. */
    public List<OpcionEnvioDto> opciones(String ubigeo) {
        exigirDistrito(ubigeo);
        List<OpcionEnvioDto> opciones = new ArrayList<>();
        for (Modalidad modalidad : Modalidad.values()) {
            if (modalidad == Modalidad.DELIVERY_LOCAL && !propiedades.esZonaLocal(ubigeo)) {
                continue;
            }
            buscar(modalidad, ubigeo).ifPresent(t -> opciones.add(
                    new OpcionEnvioDto(modalidad, t.getCosto(), t.getPlazoDiasMin(), t.getPlazoDiasMax())));
        }
        return opciones;
    }

    public CotizacionEnvioDto cotizar(Modalidad modalidad, String ubigeo, Long agenciaId) {
        exigirDistrito(ubigeo);
        if (modalidad == Modalidad.DELIVERY_LOCAL && !propiedades.esZonaLocal(ubigeo)) {
            throw new SolicitudInvalidaException("ubigeo",
                    "El delivery local solo llega a Lima Metropolitana y el Callao");
        }
        String agenciaNombre = null;
        if (modalidad == Modalidad.AGENCIA && agenciaId != null) {
            agenciaNombre = agenciaActiva(agenciaId).getNombre();
        }
        TarifaEnvio tarifa = buscar(modalidad, ubigeo).orElseThrow(() -> new FitstoreException(
                HttpStatus.UNPROCESSABLE_CONTENT, "SIN_COBERTURA", MENSAJE_SIN_COBERTURA));
        return new CotizacionEnvioDto(tarifa.getCosto(), tarifa.getPlazoDiasMin(), tarifa.getPlazoDiasMax(),
                agenciaNombre, NivelTarifa.de(tarifa.getUbigeo()), tarifa.getUbigeo());
    }

    private Optional<TarifaEnvio> buscar(Modalidad modalidad, String ubigeo) {
        return tarifas.masEspecifica(modalidad, ubigeo, ubigeo.substring(0, 4), ubigeo.substring(0, 2));
    }

    private Agencia agenciaActiva(Long agenciaId) {
        return agencias.findById(agenciaId).filter(Agencia::isActiva)
                .orElseThrow(() -> new SolicitudInvalidaException("agenciaId", "Elige una agencia disponible"));
    }

    private static void exigirDistrito(String ubigeo) {
        if (ubigeo == null || !ubigeo.matches("[0-9]{6}")) {
            throw new SolicitudInvalidaException("ubigeo", MENSAJE_ELIGE_DISTRITO);
        }
    }
}
