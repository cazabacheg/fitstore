# 11. Seguridad: JWT RS256, solo auth firma

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** **solo `auth-service` firma** JWT con su clave privada RS256 y publica un JWKS; gateway y servicios validan con la clave pública (Spring Security OAuth2 Resource Server). Access token corto (15 min) + refresh token. Roles en claims; `@PreAuthorize` en servicios. Comunicación interna servicio-a-servicio con token propagado.
- **Alternativa:** HS256 con secreto compartido (más simple, pero cualquier servicio podría firmar tokens).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
