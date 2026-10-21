package com.fitstore.delivery.service;

import com.fitstore.delivery.dto.AgenciaDto;
import com.fitstore.delivery.repository.AgenciaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agencias de transporte: lectura del checkout y mantenimiento del ADMIN (CU-28, CA-6-13 a CA-6-15). */
@Service
@Transactional
public class AgenciaService {

    private final AgenciaRepository agencias;

    public AgenciaService(AgenciaRepository agencias) {
        this.agencias = agencias;
    }

    /** Las agencias que ve el cliente en el checkout: solo las activas, en su orden. */
    @Transactional(readOnly = true)
    public List<AgenciaDto> listarActivas() {
        return agencias.findByActivaTrueOrderByOrdenAsc().stream().map(AgenciaDto::de).toList();
    }
}
