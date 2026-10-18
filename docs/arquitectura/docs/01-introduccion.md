## Introducción

**FitStore** es una plantilla de comercio electrónico *white-label* especializada en ropa y moda para MYPEs de confección. Se instala una vez por cliente (single-tenant) y se adapta a su marca desde un backoffice. El primer caso es una MYPE de Gamarra que fabrica ropa deportiva.

### Documentos fuente

- Análisis macro: `docs/analisis/01-analisis-macro.md` (visión, casos de uso, requisitos, bounded contexts, ADRs, riesgos).
- Análisis micro por servicio: `docs/analisis/micro/*.md` (modelo de dominio, datos, casos de uso, estados, eventos, dependencias, seguridad, pruebas).
- Decisiones de arquitectura: `docs/adr/` (una por archivo; también visibles en la pestaña *Decisions*).

### Vistas de este modelo

| Vista | Qué muestra |
|---|---|
| C1-Contexto | FitStore, sus usuarios y los sistemas externos |
| C2-General | Vista de conjunto: entrada, servicios y mensajería |
| C2-Datos | Persistencia: una base por servicio, Redis y Garage |
| C2-Pagos | Tokenización en el navegador, cargo en el backend y webhooks |
| C2-Sincrono | Llamadas síncronas entre servicios (OpenFeign + Resilience4j) |
| C2-Eventos | Eventos de dominio de la saga coreografiada (RabbitMQ + outbox) |
| C2-Plataforma | Configuración, descubrimiento y trazas |
| Despliegue-Nube / Despliegue-Local | Dónde corre cada contenedor |
