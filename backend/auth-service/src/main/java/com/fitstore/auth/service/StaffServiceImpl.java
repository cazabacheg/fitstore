package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.PerfilStaffRequest;
import com.fitstore.auth.dto.response.StaffResponse;
import com.fitstore.auth.mapper.StaffMapper;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.StaffRepository;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository personal;
    private final StaffMapper mapper;

    public StaffServiceImpl(StaffRepository personal, StaffMapper mapper) {
        this.personal = personal;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse miPerfil(UUID staffId) {
        return mapper.aRespuesta(buscar(staffId));
    }

    @Override
    @Transactional
    public StaffResponse actualizarMiPerfil(UUID staffId, PerfilStaffRequest solicitud) {
        Staff staff = buscar(staffId);
        mapper.actualizar(solicitud, staff);
        return mapper.aRespuesta(staff);
    }

    private Staff buscar(UUID id) {
        return personal.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No encontramos lo que buscas"));
    }
}
