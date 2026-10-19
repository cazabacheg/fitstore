# FitStore — Contratos de API y de eventos

> Versión: **v2 (borrador)** — 2026-10-19 · Estados: ✅ decidido · 🟡 propuesto · ❓ abierto
> Etapa: specs ✅ → **contratos** → diseño → estrategia multiagente → implementación.

**Historial:**
- v2 (2026-10-19): **OpenAPI 3.0.3** (soporte completo de OpenAPI Generator; 3.1 sigue experimental); K2 ✅ controladores a mano + `oasdiff` en CI; K13 TDD como barrera en la implementación.
- v1 (2026-10-19): convenciones, componentes comunes y primer contrato (auth) para revisión.

## 1. Qué hay aquí

| Ruta | Contenido |
|---|---|
| `openapi/comun.yaml` | Componentes compartidos: envoltorio `ApiResponse`, errores, página, seguridad, cabeceras y parámetros comunes |
| `openapi/<servicio>.yaml` | Un contrato **OpenAPI 3.0.3** por servicio de negocio (7): las rutas tal como las expone el gateway |
| `asyncapi/fitstore-eventos.yaml` | Contrato **AsyncAPI 3.0** con todos los eventos del exchange `fitstore.eventos` |
| `redocly.yaml` | Reglas de validación (lint) de los OpenAPI |

Fuente de verdad: los **micros** (secciones 4 y 6) y las **specs** (criterios y pantallas). Si un contrato contradice a una spec, se corrige el contrato o se reabre la spec; nunca se deja la diferencia.

## 2. Para qué sirven

1. **Trabajo en paralelo:** cada spec implementa su servicio contra los contratos de los demás, sin esperar a que existan (regla de convivencia del README de specs).
2. **Clientes del frontend generados:** `openapi-generator` (`typescript-angular`) produce `frontend/projects/shared/api/<servicio>/` desde estos archivos (SPEC-0b R5).
3. **Mocks:** WireMock para las pruebas de los clientes Feign entre servicios, y para el frontend mientras un servicio no existe.
4. **Documentación:** Swagger UI del gateway muestra lo que exponen los servicios; el CI compara eso con estos archivos (K2).

## 3. Convenciones

| # | Convención | Detalle |
|---|---|---|
| K1 | **Primero el contrato** ✅ | Se escribe y aprueba el contrato antes del código. Los cambios entran por PR que toca el contrato y el servicio a la vez |
| K2 | **Verificación contra el código** ✅ | Los controladores se escriben a mano (records, MapStruct y convenciones del ADR-015). En CI, `oasdiff` compara el OpenAPI que genera springdoc en el servicio (configurado en 3.0, `springdoc.api-docs.version=openapi_3_0`) con el de `contracts/`: cualquier diferencia que rompa el contrato falla el build |
| K3 | **Rutas** ✅ | Completas, como las ve el cliente a través del gateway: `/api/v1/...`. Un archivo por servicio, con solo sus rutas (las internas `/internal/**` van en una sección aparte del mismo archivo, con la etiqueta `interno`) |
| K4 | **Envoltorio** ✅ | Toda respuesta JSON es `ApiResponse`: `{success, mensaje, data}` (ADR-015). Cada operación declara su `data` con `allOf` sobre `ApiResponse`. Excepciones: el JWKS (formato estándar) y las descargas de archivos (PDF, CSV) |
| K5 | **Errores** ✅ | `ApiError`: `{success: false, mensaje, codigo, errores[{campo, mensaje}], requestId}`. `codigo` es estable y en mayúsculas (`CORREO_DUPLICADO`, `SIN_STOCK`, `PRECIO_CAMBIADO`) para que el frontend decida sin leer el texto; `errores` solo en `400` de validación. Respuestas usadas: 400, 401, 402 (pago rechazado), 403, 404, 409, 422 (sin cobertura de envío), 423 (cuenta bloqueada), 429, 503. Los esquemas comunes llevan `title` para que el generador les ponga su nombre (`ApiError`, `ErrorCampo`…) |
| K6 | **Paginación** ✅ | Parámetros `page` (desde 0), `size` (por defecto 20, máximo 100) y `sort` (`campo,asc\|desc`). Respuesta `data`: `{contenido[], pagina, tamano, totalElementos, totalPaginas}`; los listados con contadores agregan `contadores` (mapa estado → número) |
| K7 | **Nombres y tipos** ✅ | Propiedades en `camelCase` en español, como el código. Fechas y horas en ISO 8601 con zona (`date-time`, guardadas en UTC); fechas sin hora como `date`. Montos como número con 2 decimales (`multipleOf: 0.01`), en PEN con IGV. Ids: `uuid` para entidades que viajan entre servicios, `int64` para las internas (p. ej. direcciones, categorías) |
| K8 | **Seguridad** ✅ | `bearerAuth` (JWT RS256 del access token) en todo lo protegido; `refreshCookie` (cookie `fs_refresh`, `HttpOnly`) solo en `/auth/refresh` y `/auth/logout`. Cada operación declara su rol en la descripción y con la extensión `x-roles` (`CLIENTE`, `OPERADOR`, `ADMIN`, `PUBLICO`) |
| K9 | **Cabeceras comunes** ✅ | `X-Request-Id` en todas las respuestas (correlación); `Idempotency-Key` en el checkout y en los cobros; `X-Carrito-Id` en el carrito anónimo; `Retry-After` en el `429` |
| K10 | **Versión** ✅ | `info.version` semántica (`1.0.0`); un cambio incompatible exige `/api/v2` (no previsto en el proyecto) |
| K11 | **Ejemplos** ✅ | Cada operación lleva al menos un ejemplo realista en español (los mismos datos de demostración de las specs), que usan WireMock y la documentación |
| K12 | **Eventos** ✅ | AsyncAPI 3.0 con un canal por routing key (`{servicio}.{entidad}.{acción}`), el sobre común (`eventoId`, `tipo`, `version`, `ocurridoEn`) y el `payload` de cada evento según el micro de su servicio. Cada mensaje indica quién lo publica y quién lo consume |
| K13 | **TDD como barrera** ✅ | La implementación es guiada por las specs y usa TDD: cada criterio de aceptación empieza como una prueba que falla (rojo → verde → refactor). Los ejemplos de los contratos se reutilizan en las pruebas de API (MockMvc) y en los stubs de WireMock, y la prueba de `oasdiff` asegura que el código no se aparte del contrato. El detalle va en la estrategia multiagente |

## 4. Validación

Versiones fijadas (con más de dos semanas de publicadas):

| Herramienta | Versión | Uso |
|---|---|---|
| Redocly CLI | `@redocly/cli@2.57.0` | `npx @redocly/cli@2.57.0 lint --config contracts/redocly.yaml contracts/openapi/*-service.yaml` en `ci.yml` (SPEC-0a) |
| AsyncAPI CLI | `@asyncapi/cli@6.2.0` | `npx @asyncapi/cli@6.2.0 validate contracts/asyncapi/fitstore-eventos.yaml` |
| OpenAPI Generator | `openapitools/openapi-generator-cli:v7.10.0` | Clientes `typescript-angular` (SPEC-0b R5). Con OpenAPI 3.0.3 no hay avisos; se verificó con auth que genera bien el envoltorio `allOf`, los nulos (`string \| null`), los enums, las páginas con nombre y las referencias a `comun.yaml` |
| oasdiff | se fija en la SPEC-0a | `oasdiff breaking` entre el contrato y el OpenAPI generado por cada servicio (K2) |

## 5. Orden de trabajo

1. Convenciones, `comun.yaml` y **auth** (este PR) → revisión.
2. catalog, settings, order, payment, delivery, notification (en un PR, revisados por servicio).
3. AsyncAPI de eventos.
4. Revisión de coherencia contratos ↔ micros ↔ specs y cierre de la etapa.
