# 4. `settings-service` para la configuración de tienda

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** servicio dedicado que guarda branding, contenido, políticas y parámetros de negocio; expone `GET /api/v1/settings/public` (sin auth, cacheable) y endpoints de administración (ADMIN). Publica `ConfiguracionActualizada`.
- **Frontend:** los frontends cargan la configuración al iniciar (`APP_INITIALIZER`) y escriben los colores como **CSS custom properties** (`--fs-color-primary`, ...). Tailwind (storefront) y Angular Material 3 (backoffice) se configuran para leer esas variables → cambio de tema en caliente.
- **Separación:** config-server = configuración técnica y secretos por entorno/cliente; settings-service = configuración de negocio editable por el cliente.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
