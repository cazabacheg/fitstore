-- Quién hizo cada cambio, para mostrar "con mi usuario" en el historial del backoffice (CA-4-44).
-- El nombre sale del claim "nombre" del JWT del staff; es nulo cuando el actor es el sistema.
ALTER TABLE pedido_historial ADD COLUMN actor_nombre varchar(120);
