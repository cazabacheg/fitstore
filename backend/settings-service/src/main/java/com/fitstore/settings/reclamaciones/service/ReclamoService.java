package com.fitstore.settings.reclamaciones.service;

import com.fitstore.common.api.PaginaResponse;
import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.dto.ReclamoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRegistradoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRequest;
import com.fitstore.settings.reclamaciones.dto.ReclamoResumenDto;
import org.springframework.data.domain.Pageable;

/** Libro de Reclamaciones: registro público y atención del staff (CU-14, CU-34). */
public interface ReclamoService {

    /**
     * Entrega el siguiente número del año (atómico y sin huecos), calcula los 15 días hábiles y publica
     * {@code ReclamoRegistrado}. Todo en una transacción: si algo falla, el número no se consume.
     *
     * @param ip IP de quien registra (para el staff)
     */
    ReclamoRegistradoDto registrar(ReclamoRequest solicitud, String ip);

    PaginaResponse<ReclamoResumenDto> listar(FiltroReclamos filtro, Pageable paginacion);

    ReclamoDto obtener(Long id);

    /** REGISTRADO pasa a EN_ATENCION. @throws com.fitstore.common.api.ConflictoNegocioException {@code ESTADO_INVALIDO} */
    ReclamoDto tomar(Long id);

    /** REGISTRADO o EN_ATENCION pasan a RESPONDIDO y se publica {@code ReclamoRespondido}; luego es inmutable. */
    ReclamoDto responder(Long id, String respuesta, String staffId);
}
