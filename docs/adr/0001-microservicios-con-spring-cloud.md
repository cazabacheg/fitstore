# 1. Microservicios con Spring Cloud

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Contexto:** requisito académico de Spring + deseo explícito de una arquitectura de microservicios completa.
- **Decisión:** 10 servicios: `config-server`, `discovery-server` (Eureka), `api-gateway` (Spring Cloud Gateway) + 7 servicios de negocio (ver ADR-019). Spring Boot 4.0.x + Spring Cloud 2025.1.x, Java 21.
- **Alternativas:** monolito modular (más simple y barato, descartado por objetivo de aprendizaje y exposición).
- **Consecuencias:** más consumo de memoria y complejidad operativa; se compensa con docker-compose, convenciones comunes y un módulo `common`.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
