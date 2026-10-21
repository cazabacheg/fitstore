package com.fitstore.payment.service;

import com.fitstore.payment.model.Boleta;
import java.util.UUID;

/** Boleta simulada en PDF: emisión asíncrona con correlativo, almacenamiento privado y descarga (HU-5-09, T6). */
public interface BoletaService {

    /**
     * Emite la boleta de una intención cobrada: toma el siguiente correlativo, arma el PDF, lo guarda en el bucket privado y
     * registra {@code BoletaEmitida}, todo en una transacción. Si algo falla no se consume el correlativo. Si la intención ya
     * tiene boleta, devuelve esa.
     *
     * @throws IllegalStateException si falla el almacenamiento o no hay datos del emisor (se reintenta más tarde)
     */
    Boleta emitir(UUID intencionId);

    /** Emite las boletas pendientes de las intenciones ya cobradas; una que falle no detiene a las demás. @return cuántas emitió */
    int emitirPendientes();

    /** @throws com.fitstore.common.api.RecursoNoEncontradoException si el pedido es de otra clienta o aún no tiene boleta */
    DescargaBoleta descargarDeLaClienta(String codigoPedido, UUID clienteId);

    /** Para el backoffice. @throws com.fitstore.common.api.RecursoNoEncontradoException si el pago no tiene boleta */
    DescargaBoleta descargarDeLaIntencion(UUID intencionId);

    /** Para notification (adjunta el PDF al correo). @throws com.fitstore.common.api.RecursoNoEncontradoException si no existe */
    DescargaBoleta descargarPorNumero(String numero);
}
