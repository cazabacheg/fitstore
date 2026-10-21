package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.DireccionRequest;
import com.fitstore.auth.dto.response.DireccionResponse;
import com.fitstore.auth.mapper.DireccionMapper;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Direccion;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.DireccionRepository;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DireccionServiceImpl implements DireccionService {

    /** Invariante 4: máximo de direcciones por cliente. */
    static final int MAXIMO = 10;

    private final DireccionRepository direcciones;
    private final ClienteRepository clientes;
    private final DireccionMapper mapper;

    public DireccionServiceImpl(DireccionRepository direcciones, ClienteRepository clientes, DireccionMapper mapper) {
        this.direcciones = direcciones;
        this.clientes = clientes;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponse> listar(UUID clienteId) {
        return direcciones.findByClienteIdOrderByPredeterminadaDescIdAsc(clienteId).stream().map(mapper::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DireccionResponse obtener(UUID clienteId, Long id) {
        return mapper.aRespuesta(propia(clienteId, id));
    }

    @Override
    @Transactional
    public DireccionResponse crear(UUID clienteId, DireccionRequest solicitud) {
        // se bloquea la fila del cliente para que dos altas simultáneas no superen el máximo
        Cliente cliente = clientes.bloquearPorId(clienteId).orElseThrow(DireccionServiceImpl::noEncontrada);
        long actuales = direcciones.countByClienteId(clienteId);
        if (actuales >= MAXIMO) {
            throw new ConflictoNegocioException("LIMITE_DIRECCIONES", "Ya tienes " + MAXIMO + " direcciones guardadas");
        }
        Direccion nueva = new Direccion();
        mapper.actualizar(solicitud, nueva);
        nueva.setCliente(cliente);
        nueva.setPredeterminada(actuales == 0);
        return mapper.aRespuesta(direcciones.save(nueva));
    }

    @Override
    @Transactional
    public DireccionResponse actualizar(UUID clienteId, Long id, DireccionRequest solicitud) {
        Direccion direccion = propia(clienteId, id);
        mapper.actualizar(solicitud, direccion);
        return mapper.aRespuesta(direccion);
    }

    @Override
    @Transactional
    public void eliminar(UUID clienteId, Long id) {
        Direccion direccion = propia(clienteId, id);
        boolean eraPredeterminada = direccion.isPredeterminada();
        direcciones.delete(direccion);
        direcciones.flush();
        if (eraPredeterminada) {
            direcciones.findFirstByClienteIdOrderByCreadoEnDescIdDesc(clienteId).ifPresent(d -> d.setPredeterminada(true));
        }
    }

    @Override
    @Transactional
    public List<DireccionResponse> marcarPredeterminada(UUID clienteId, Long id) {
        Direccion nueva = propia(clienteId, id);
        if (!nueva.isPredeterminada()) {
            direcciones.findByClienteIdAndPredeterminadaTrue(clienteId).ifPresent(anterior -> anterior.setPredeterminada(false));
            // el índice único parcial exige soltar la anterior antes de marcar la nueva
            direcciones.flush();
            nueva.setPredeterminada(true);
        }
        return listar(clienteId);
    }

    /** A1: siempre por {@code cliente_id} del token; si no existe o es de otro cliente, 404. */
    private Direccion propia(UUID clienteId, Long id) {
        return direcciones.findByIdAndClienteId(id, clienteId).orElseThrow(DireccionServiceImpl::noEncontrada);
    }

    private static RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException("No encontramos lo que buscas");
    }
}
