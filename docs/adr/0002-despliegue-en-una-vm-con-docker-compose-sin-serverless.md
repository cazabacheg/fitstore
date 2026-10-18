# 2. Despliegue en una VM con docker-compose, sin serverless

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** una VM de ~8 GB (Azure for Students; AWS como alternativa) con docker-compose y Caddy como proxy inverso con HTTPS automático.
- **Alternativas:** Lambda (rompe Eureka/Gateway, cold starts), ECS Fargate (sin escalar a cero, más caro con 12 tareas), App Runner (cerrado a nuevos clientes), Kubernetes (excesivo).
- **Consecuencias:** un solo punto de fallo (aceptable en contexto académico/MYPE); despliegue simple y reproducible.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
