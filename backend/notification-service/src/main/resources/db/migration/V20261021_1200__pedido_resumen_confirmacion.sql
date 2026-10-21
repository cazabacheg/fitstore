-- SPEC-7 (micro 4.3): respaldo de 10 minutos de la confirmación del pedido.
-- PedidoPagado marca cuándo se pagó el pedido; la confirmación (con la boleta, o sin ella tras el respaldo) se envía una
-- sola vez, y `confirmacion_enviada` evita una segunda si la boleta llega después del respaldo.
ALTER TABLE pedido_resumen
    ADD COLUMN pagado_en            timestamptz,
    ADD COLUMN confirmacion_enviada boolean NOT NULL DEFAULT false;

CREATE INDEX ix_pedido_resumen_sin_confirmar ON pedido_resumen (pagado_en)
    WHERE pagado_en IS NOT NULL AND NOT confirmacion_enviada;
