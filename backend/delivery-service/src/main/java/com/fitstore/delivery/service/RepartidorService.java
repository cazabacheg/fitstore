package com.fitstore.delivery.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.RepartidorDto;
import com.fitstore.delivery.dto.RepartidorRequest;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import com.fitstore.delivery.repository.EntregaRepository;
import com.fitstore.delivery.repository.RepartidorRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Repartidores propios de la tienda (CU-27, CA-6-16 a CA-6-18). */
@Service
@Transactional
public class RepartidorService {

    private final RepartidorRepository repartidores;
    private final EntregaRepository entregas;

    public RepartidorService(RepartidorRepository repartidores, EntregaRepository entregas) {
        this.repartidores = repartidores;
        this.entregas = entregas;
    }

    @Transactional(readOnly = true)
    public List<RepartidorDto> listar(Boolean activo) {
        return repartidores.findAll().stream()
                .filter(r -> activo == null || r.isActivo() == activo)
                .sorted(Comparator.comparing(Repartidor::getNombres, String.CASE_INSENSITIVE_ORDER))
                .map(this::aDto)
                .toList();
    }

    public RepartidorDto crear(RepartidorRequest pedido) {
        Repartidor repartidor = Repartidor.builder().build();
        aplicar(repartidor, pedido);
        return RepartidorDto.de(repartidores.save(repartidor), false);
    }

    public RepartidorDto editar(Long id, RepartidorRequest pedido) {
        Repartidor repartidor = buscar(id);
        aplicar(repartidor, pedido);
        return aDto(repartidor);
    }

    /** Desactivar no toca las entregas anteriores: siguen apuntando al repartidor (CA-6-17). */
    public RepartidorDto cambiarActivo(Long id, boolean activo) {
        Repartidor repartidor = buscar(id);
        repartidor.setActivo(activo);
        return aDto(repartidor);
    }

    public void eliminar(Long id) {
        Repartidor repartidor = buscar(id);
        if (entregas.existsByRepartidorId(id)) {
            throw new ConflictoNegocioException("REPARTIDOR_CON_ENTREGAS",
                    "El repartidor tiene entregas; desactívalo en vez de eliminarlo");
        }
        repartidores.delete(repartidor);
    }

    private void aplicar(Repartidor repartidor, RepartidorRequest pedido) {
        boolean pidePlaca = pedido.vehiculo() != TipoVehiculo.BICICLETA;
        if (pidePlaca && (pedido.placa() == null || pedido.placa().isBlank())) {
            throw new SolicitudInvalidaException("placa", "La placa es obligatoria para moto y auto");
        }
        repartidor.setNombres(pedido.nombres().trim());
        repartidor.setTelefono(pedido.telefono());
        repartidor.setVehiculo(pedido.vehiculo());
        repartidor.setPlaca(pidePlaca ? pedido.placa() : null);
    }

    private Repartidor buscar(Long id) {
        return repartidores.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos a ese repartidor"));
    }

    private RepartidorDto aDto(Repartidor repartidor) {
        return RepartidorDto.de(repartidor, entregas.existsByRepartidorId(repartidor.getId()));
    }
}
