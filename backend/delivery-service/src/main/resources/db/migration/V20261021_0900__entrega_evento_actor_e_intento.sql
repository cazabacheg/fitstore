-- SPEC-6: el seguimiento muestra el nombre de quien hizo cada paso y el intento en el que ocurrió (contrato EventoSeguimiento).
ALTER TABLE entrega_evento
    ADD COLUMN actor_nombre varchar(120),
    ADD COLUMN intento      integer NOT NULL DEFAULT 0;
