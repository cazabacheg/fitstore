package com.fitstore.delivery.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.delivery.config.PropiedadesDelivery;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.TarifaDto;
import com.fitstore.delivery.dto.TarifaRequest;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.repository.TarifaEnvioRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantenimiento de las tarifas de envío por el ADMIN (CU-28, CA-6-07 a CA-6-10). */
@Service
@Transactional
public class TarifaService {

    private final TarifaEnvioRepository tarifas;
    private final PropiedadesDelivery propiedades;

    public TarifaService(TarifaEnvioRepository tarifas, PropiedadesDelivery propiedades) {
        this.tarifas = tarifas;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public List<TarifaDto> listar(Modalidad modalidad, NivelTarifa nivel) {
        List<TarifaEnvio> todas = modalidad == null ? tarifas.findAll() : tarifas.findByModalidad(modalidad);
        return todas.stream()
                .filter(t -> nivel == null || NivelTarifa.de(t.getUbigeo()) == nivel)
                .sorted(Comparator.comparing(TarifaEnvio::getModalidad).thenComparing(TarifaEnvio::getUbigeo))
                .map(TarifaDto::de)
                .toList();
    }

    public TarifaDto crear(TarifaRequest pedido) {
        validar(pedido);
        if (tarifas.existsByModalidadAndUbigeo(pedido.modalidad(), pedido.ubigeo())) {
            throw duplicada();
        }
        TarifaEnvio tarifa = TarifaEnvio.builder().build();
        aplicar(tarifa, pedido);
        return TarifaDto.de(tarifas.save(tarifa));
    }

    public TarifaDto editar(Long id, TarifaRequest pedido) {
        TarifaEnvio tarifa = tarifas.findById(id).orElseThrow(TarifaService::noEncontrada);
        validar(pedido);
        if (tarifas.existsByModalidadAndUbigeoAndIdNot(pedido.modalidad(), pedido.ubigeo(), id)) {
            throw duplicada();
        }
        aplicar(tarifa, pedido);
        return TarifaDto.de(tarifas.save(tarifa));
    }

    /** DELETE físico: los pedidos ya creados guardan su propio costo (CA-6-10). */
    public void eliminar(Long id) {
        tarifas.delete(tarifas.findById(id).orElseThrow(TarifaService::noEncontrada));
    }

    private void validar(TarifaRequest pedido) {
        if (pedido.ubigeo() == null || !pedido.ubigeo().matches("[0-9]{2}|[0-9]{4}|[0-9]{6}")) {
            throw new SolicitudInvalidaException("ubigeo", "Elige un departamento, una provincia o un distrito");
        }
        if (pedido.modalidad() == Modalidad.DELIVERY_LOCAL && !soloLimaYCallao(pedido.ubigeo())) {
            throw new SolicitudInvalidaException("ubigeo",
                    "El delivery local solo cubre Lima Metropolitana y el Callao");
        }
        if (pedido.plazoDiasMin() > pedido.plazoDiasMax()) {
            throw new SolicitudInvalidaException("plazoDiasMax", "El plazo máximo no puede ser menor que el mínimo");
        }
    }

    /** Un departamento o provincia solo sirve para el local si todo su territorio es local: Callao (07) o 1501. */
    private boolean soloLimaYCallao(String ubigeo) {
        return propiedades.local().prefijosUbigeo().stream().anyMatch(ubigeo::startsWith);
    }

    private static void aplicar(TarifaEnvio tarifa, TarifaRequest pedido) {
        tarifa.setModalidad(pedido.modalidad());
        tarifa.setUbigeo(pedido.ubigeo());
        tarifa.setCosto(pedido.costo());
        tarifa.setPlazoDiasMin(pedido.plazoDiasMin());
        tarifa.setPlazoDiasMax(pedido.plazoDiasMax());
        tarifa.setActiva(pedido.activa() == null || pedido.activa());
    }

    private static ConflictoNegocioException duplicada() {
        return new ConflictoNegocioException("TARIFA_DUPLICADA", "Ya existe una tarifa para ese destino");
    }

    private static RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException("No encontramos esa tarifa");
    }
}
