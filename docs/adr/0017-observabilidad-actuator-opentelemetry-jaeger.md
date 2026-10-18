# 17. Observabilidad: Actuator + OpenTelemetry + Jaeger

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** Spring Boot Actuator (health, info, metrics) + **Micrometer Tracing con el puente de OpenTelemetry**, exportando trazas por OTLP a **Jaeger** (all-in-one, almacenamiento en memoria, ~150–250 MB). `traceId` en todos los logs.
- **Zipkin vs Jaeger:** cumplen la misma función (visualizar el recorrido de una petición entre servicios); se usa **uno solo**. Se prefiere Jaeger porque se integra de forma nativa con OpenTelemetry (estándar de la industria) y el mismo exportador OTLP sirve después para Azure Monitor o AWS X-Ray sin cambiar código.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
