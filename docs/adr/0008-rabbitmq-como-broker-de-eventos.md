# 8. RabbitMQ como broker de eventos

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** RabbitMQ con exchanges *topic* por contexto, colas por consumidor, *dead-letter queues* y reintentos. Integración con Spring AMQP.
- **Alternativas:** Kafka (descartado: sobredimensionado para el volumen, más pesado en memoria), solo llamadas síncronas (acopla y no permite sagas limpias).
- **Consecuencias:** ~150–250 MB de RAM; consola de administración útil para la demo.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
