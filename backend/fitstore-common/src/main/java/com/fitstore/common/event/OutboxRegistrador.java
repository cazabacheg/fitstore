package com.fitstore.common.event;

import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Guarda un evento en {@code outbox_evento} dentro de la transacción del servicio (ADR-009). */
public class OutboxRegistrador {

    private final OutboxEventoRepository repositorio;
    private final JsonMapper json;

    public OutboxRegistrador(OutboxEventoRepository repositorio, JsonMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    /**
     * @param tipo       nombre del evento (p. ej. {@code PedidoPagado}), que {@link CatalogoRutasEventos} traduce a routing key
     * @param agregadoId id del agregado que lo origina
     * @param datos      contenido del evento; se guarda como JSON y viaja en {@code datos} del sobre
     * @return el {@code eventoId}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID registrar(String tipo, String agregadoId, Object datos) {
        OutboxEvento evento = OutboxEvento.nuevo(tipo, agregadoId, json.writeValueAsString(datos));
        repositorio.save(evento);
        return evento.getId();
    }
}
