# 7. Migraciones con Flyway

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** esquema versionado con Flyway por servicio (`ddl-auto=validate`); datos semilla (catálogo gym wear de ejemplo) como migraciones separadas por perfil.
- **Alternativa:** `ddl-auto=update` como en la clase (rápido pero no reproducible).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
