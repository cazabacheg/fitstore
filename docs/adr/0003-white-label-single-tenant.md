# 3. White-label single-tenant

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** una instalación (VM + BDs) por cliente. La marca se configura en datos, no en código.
- **Alternativas:** multi-tenant (una instalación para muchas tiendas; abarata por cliente pero complica todo el modelo de datos y la seguridad).
- **Consecuencias:** aislamiento total entre clientes; costo por cliente = costo de una instalación (ver ADR-005).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
