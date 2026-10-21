package com.fitstore.payment.service;

import com.fitstore.payment.dto.PagoDetalle;
import com.fitstore.payment.dto.PagoResumen;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Consulta de pagos para el backoffice (HU-5-11, solo ADMIN). */
public interface AdminPagoService {

    Page<PagoResumen> listar(FiltroPagos filtro, Pageable paginacion);

    /** @throws com.fitstore.common.api.RecursoNoEncontradoException si el pago no existe */
    PagoDetalle obtener(UUID intencionId);
}
