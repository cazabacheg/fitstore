package com.fitstore.delivery.service;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.AgenciaDto;
import com.fitstore.delivery.dto.AgenciaRequest;
import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.repository.AgenciaRepository;
import com.fitstore.delivery.repository.EntregaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agencias de transporte: lectura del checkout y mantenimiento del ADMIN (CU-28, CA-6-13 a CA-6-15). */
@Service
@Transactional
public class AgenciaService {

    private static final String PLANTILLA_URL = "^https://.*\\{codigo\\}.*$";

    private final AgenciaRepository agencias;
    private final EntregaRepository entregas;

    public AgenciaService(AgenciaRepository agencias, EntregaRepository entregas) {
        this.agencias = agencias;
        this.entregas = entregas;
    }

    /** Las agencias que ve el cliente en el checkout: solo las activas, en su orden. */
    @Transactional(readOnly = true)
    public List<AgenciaDto> listarActivas() {
        return agencias.findByActivaTrueOrderByOrdenAsc().stream().map(AgenciaDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AgenciaDto> listarTodas() {
        return agencias.findAllByOrderByOrdenAscIdAsc().stream().map(AgenciaDto::de).toList();
    }

    public AgenciaDto crear(AgenciaRequest pedido) {
        validarUrl(pedido.urlSeguimiento());
        String nombre = pedido.nombre().trim();
        if (agencias.existsByNombreIgnoreCase(nombre)) {
            throw nombreRepetido();
        }
        int siguiente = agencias.findAllByOrderByOrdenAscIdAsc().stream().mapToInt(Agencia::getOrden).max().orElse(-1) + 1;
        Agencia agencia = Agencia.builder().nombre(nombre).urlSeguimiento(vacioANulo(pedido.urlSeguimiento()))
                .orden(siguiente).build();
        return AgenciaDto.de(agencias.save(agencia));
    }

    public AgenciaDto editar(Long id, AgenciaRequest pedido) {
        Agencia agencia = buscar(id);
        validarUrl(pedido.urlSeguimiento());
        String nombre = pedido.nombre().trim();
        if (agencias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreRepetido();
        }
        agencia.setNombre(nombre);
        agencia.setUrlSeguimiento(vacioANulo(pedido.urlSeguimiento()));
        return AgenciaDto.de(agencia);
    }

    public AgenciaDto cambiarActiva(Long id, boolean activa) {
        Agencia agencia = buscar(id);
        agencia.setActiva(activa);
        return AgenciaDto.de(agencia);
    }

    /** Mueve la agencia a la posición pedida (desde 0) y renumera todas, como el arrastre del backoffice (CA-6-14). */
    public List<AgenciaDto> ordenar(Long id, int posicion) {
        Agencia movida = buscar(id);
        List<Agencia> lista = new ArrayList<>(agencias.findAllByOrderByOrdenAscIdAsc());
        lista.removeIf(a -> a.getId().equals(movida.getId()));
        lista.add(Math.min(posicion, lista.size()), movida);
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).setOrden(i);
        }
        return lista.stream().map(AgenciaDto::de).toList();
    }

    public void eliminar(Long id) {
        Agencia agencia = buscar(id);
        if (entregas.existsByAgenciaId(id)) {
            throw new ConflictoNegocioException("AGENCIA_CON_ENTREGAS",
                    "La agencia tiene entregas; desactívala en vez de eliminarla");
        }
        agencias.delete(agencia);
    }

    private Agencia buscar(Long id) {
        return agencias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos esa agencia"));
    }

    private static void validarUrl(String url) {
        if (url != null && !url.isBlank() && !url.matches(PLANTILLA_URL)) {
            throw new SolicitudInvalidaException("urlSeguimiento", "La URL debe ser https e incluir {codigo}");
        }
    }

    private static String vacioANulo(String url) {
        return url == null || url.isBlank() ? null : url.trim();
    }

    private static ConflictoNegocioException nombreRepetido() {
        return new ConflictoNegocioException("AGENCIA_DUPLICADA", "Ya existe una agencia con ese nombre");
    }
}
