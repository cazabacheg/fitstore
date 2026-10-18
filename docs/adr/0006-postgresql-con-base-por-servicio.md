# 6. PostgreSQL con base por servicio

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** PostgreSQL 17 (una instancia, una base lógica por servicio, usuario por servicio sin acceso cruzado).
- **Alternativas:** SQL Server (lo usa la clase; ~2 GB de RAM en contenedor, licencia en nube), MySQL (lo menciona la ficha DSW; viable).
- **Consecuencias:** liviano, estándar, gratuito en cualquier nube; JPA mantiene portabilidad. Se justifica en el informe frente a la ficha.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
