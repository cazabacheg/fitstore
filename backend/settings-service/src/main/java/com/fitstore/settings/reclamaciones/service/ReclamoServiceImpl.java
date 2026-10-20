package com.fitstore.settings.reclamaciones.service;

import com.fitstore.common.api.PaginaResponse;
import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.dto.ReclamoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRegistradoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRequest;
import com.fitstore.settings.reclamaciones.dto.ReclamoResumenDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Pendiente de implementar. */
@Service
public class ReclamoServiceImpl implements ReclamoService {

    @Override
    public ReclamoRegistradoDto registrar(ReclamoRequest solicitud, String ip) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public PaginaResponse<ReclamoResumenDto> listar(FiltroReclamos filtro, Pageable paginacion) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ReclamoDto obtener(Long id) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ReclamoDto tomar(Long id) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ReclamoDto responder(Long id, String respuesta, String staffId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
