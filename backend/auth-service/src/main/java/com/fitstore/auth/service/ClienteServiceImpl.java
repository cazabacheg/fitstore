package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteDetalleResponse;
import com.fitstore.auth.dto.response.ClienteResponse;
import com.fitstore.auth.dto.response.ClienteResumenResponse;
import com.fitstore.auth.mapper.ClienteMapper;
import com.fitstore.auth.mapper.DireccionMapper;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.ClienteSpecs;
import com.fitstore.auth.repository.DireccionRepository;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clientes;
    private final DireccionRepository direcciones;
    private final ClienteMapper mapper;
    private final DireccionMapper direccionMapper;
    private final Clock reloj;

    public ClienteServiceImpl(ClienteRepository clientes, DireccionRepository direcciones, ClienteMapper mapper,
            DireccionMapper direccionMapper, Clock reloj) {
        this.clientes = clientes;
        this.direcciones = direcciones;
        this.mapper = mapper;
        this.direccionMapper = direccionMapper;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse perfil(UUID clienteId) {
        return mapper.aPerfil(buscar(clienteId));
    }

    @Override
    @Transactional
    public ClienteResponse actualizarPerfil(UUID clienteId, PerfilClienteRequest solicitud) {
        Cliente cliente = buscar(clienteId);
        mapper.actualizar(solicitud, cliente);
        return mapper.aPerfil(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResumenResponse> buscar(String texto, Boolean activo, Pageable pagina) {
        return PaginaResponse.de(clientes.findAll(ClienteSpecs.filtrar(texto, activo), pagina).map(mapper::aResumen));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDetalleResponse detalle(UUID id) {
        return detalleDe(buscar(id));
    }

    @Override
    @Transactional
    public ClienteDetalleResponse bloquear(UUID id) {
        Cliente cliente = buscar(id);
        cliente.setActivo(false);
        // invalida sus sesiones: ya no puede refrescar (invariantes 5 y 8)
        cliente.setCredencialesActualizadasEn(reloj.instant().truncatedTo(ChronoUnit.MILLIS));
        return detalleDe(cliente);
    }

    @Override
    @Transactional
    public ClienteDetalleResponse desbloquear(UUID id) {
        Cliente cliente = buscar(id);
        cliente.setActivo(true);
        return detalleDe(cliente);
    }

    private ClienteDetalleResponse detalleDe(Cliente c) {
        var lista = direcciones.findByClienteIdOrderByPredeterminadaDescIdAsc(c.getId()).stream()
                .map(direccionMapper::aRespuesta).toList();
        return new ClienteDetalleResponse(c.getId(), c.getEmail(), c.getNombres(), c.getApellidos(), c.getDni(),
                c.getTelefono(), c.isActivo(), c.getCreadoEn(), c.getUltimoLoginEn(), lista);
    }

    private Cliente buscar(UUID id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos lo que buscas"));
    }
}
