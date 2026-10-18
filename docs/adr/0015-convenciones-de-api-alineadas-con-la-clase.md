# 15. Convenciones de API alineadas con la clase

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** `/api/v1/<recurso>`, envoltorio `ApiResponse<T>` (`success`, `mensaje`, `data`), `201 Created` con `Location`, `GlobalExceptionHandler`, borrado lógico con `activo`, paquetes `controller/service(+impl)/repository/model/dto/exception`. Paginación estándar `page/size/sort`. OpenAPI por servicio agregado en el gateway.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
