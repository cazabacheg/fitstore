package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilClienteRequest;
import com.fitstore.auth.dto.response.ClienteResponse;
import com.fitstore.auth.mapper.ClienteMapper;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clientes;
    private final ClienteMapper mapper;

    public ClienteServiceImpl(ClienteRepository clientes, ClienteMapper mapper) {
        this.clientes = clientes;
        this.mapper = mapper;
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

    private Cliente buscar(UUID id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos lo que buscas"));
    }
}
