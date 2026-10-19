# Checkpoint CK1 — Base de 0a.1

> Lead · 2026-10-19 · Revisión exhaustiva antes de lanzar los 7 esqueletos en paralelo ([estrategia 2.1](../estrategia-multiagente.md#21-checkpoints-de-base)).
> Base revisada: `dev` en `f669713` (PR #17, paso 0a.1 de la SPEC-0a v6). Contraste: **spec primero**, luego el micro de infraestructura v8.

## Resultado

**Aprobable con correcciones.** La base cumple la SPEC-0a en lo que corresponde a 0a.1 y está bien probada. Se encontró **un bloqueante para el trabajo en paralelo** (MapStruct y procesadores fuera del POM padre) y tres correcciones menores; todas se corrigen en `fix/ck1-base` y en este PR de documentación **antes** de lanzar 0a.2.

## 1. Desde cero

| Verificación | Resultado |
|---|---|
| `git clone` limpio de `dev` + `./mvnw -B -fae clean verify` | ✅ BUILD SUCCESS, 11 módulos, **147 pruebas** sin fallos (common 55, config-server 3, discovery-server 3, api-gateway 79, 7 de arranque) |
| JaCoCo `check` (80 % de líneas) | ✅ common 92,8 %, gateway 99,2 % |
| Compose de dev (infraestructura + config + Eureka + gateway) | ✅ todo *healthy*; el código integrado es idéntico al que se levantó en la verificación del PR #17 |
| Gateway en vivo | ✅ `/internal/**` → 404 · token adulterado → 401 · sin instancias → 503 `ApiError` · `X-Request-Id` · 6.º login → 429 · Swagger con 7 servicios · Jaeger recibe trazas |
| Memoria del compose (`mem_limit`) | ✅ 4,4 GB sumados (límite RNF-03: 6,5 GB) |

## 2. Código de la base contra la spec y el micro

| Pieza | Spec / micro | Resultado |
|---|---|---|
| POM padre | SPEC-0a "Incluye" 1, I7; stack-versiones | ⚠️ **H1** falta MapStruct 1.6.3, `lombok-mapstruct-binding` 0.2.0 y los procesadores de anotaciones en el orden de stack-versiones. El POM padre queda congelado tras 0a, así que los servicios no podrían configurarlos. **Bloqueante para 0a.2.** Resto ✅ (Boot 4.0.8, Cloud 2025.1.3, Testcontainers 2.0.5, JaCoCo 0.8.15, `spring-retry` 2.0.13) |
| `fitstore-common` | Infraestructura 7 y 10.3 | ✅ paquetes `api`, `security`, `event`, `storage`, `feign` (+ Resilience4j base), `money`, `autoconfigure`; sin lógica de negocio; cada pieza se activa por propiedades |
| Seguridad base | I3, auth micro (claims `sub`, `tipo`, `rol`) | ✅ todo cerrado salvo `health` y documentación; `ROLE_ADMIN > ROLE_OPERADOR`; JWKS de prueba solo con `fitstore.security.jwks-archivo` |
| Outbox y consumidor | ADR-009, infraestructura 8 | ✅ marca `publicado_en` solo con *publisher confirm*; consumidor idempotente en la misma transacción, 3 intentos y DLQ |
| Gateway | Infraestructura 4 y 10.1 | ✅ cadena Correlación → JWT → bloqueo interno → rate limiting → rutas; 503 con `ApiError`; CORS por configuración. ⚠️ **H2** `SeguridadJwtConfig` está en el paquete `config`, que JaCoCo excluye (I7): núcleo fuera de la medición. ⚠️ **H3** Javadoc de `RateLimitingFilter` desactualizado |
| Rutas del gateway | Infraestructura 4.2 y **todos los paths de los 7 contratos** | ✅ cada ruta pública de `contracts/openapi/*-service.yaml` llega a su servicio (el gateway queda congelado, por eso se revisó contra los contratos y no solo contra la tabla del micro) |
| `X-Request-Id` | K9, CA-0a-13 | ✅ el gateway genera el id y reemplaza el del cliente. ⚠️ **H4** el micro decía "genera o propaga": se aclara en el micro y en el C3 |
| Compose | Infraestructura 6 | ✅ imágenes fijas (stack-versiones v5), `mem_limit`, *healthchecks*, orden de 6.2 |
| `init.sql` | ADR-006, CA-0a-07 | ✅ 7 bases y 7 usuarios; `REVOKE CONNECT … FROM PUBLIC` |
| `.env.example` | Infraestructura 10.5 | ✅ todas las variables, valores solo de dev |
| SPEC-0a, sección 8 | B2 punto 4 | ⚠️ **H5** la sección 6 pide `infra/scripts/ubigeo-a-sql`, pero la sección 8 no lo listaba (tampoco `.dockerignore`): se corrige la spec (v7) |

## 3. Contratos

`ApiResponse` `{success, mensaje, data}` (K4), `ApiError` `{success:false, mensaje, codigo, errores[{campo, mensaje}], requestId, detalles}` omitiendo los vacíos (K5), `PaginaResponse` `{contenido, pagina, tamano, totalElementos, totalPaginas}` (K6; los listados con `contadores` los agregan en su propio DTO) y sobre de eventos `{eventoId, tipo, version, ocurridoEn, datos}` (K12): ✅ idénticos a `contracts/`.

## 4. Pruebas y cobertura

Se leyeron los nombres y el propósito de las **147 pruebas**: cada criterio de 0a.1 tiene casos positivos, negativos y de borde (por ejemplo, CA-0a-10 con token válido, inválido, vencido, mal formado y en ruta pública; CA-0a-14 con confirmación, sin confirmación, RabbitMQ caído y recuperado, tipo sin routing key). Núcleo cubierto, salvo las ramas de `InterruptedException` (justificadas: solo restauran el flag de interrupción).

## 5. Seguridad

✅ Sin secretos versionados; `.env` ignorado; la clave de `infra/dev/clave-privada-PRUEBA-solo-dev.jwk.json` está marcada como de prueba (0a.3 la pone en la *allowlist* de gitleaks). Observación para la spec 8: la IP del rate limiting se toma de `X-Forwarded-For`; Caddy debe reescribir esa cabecera (no reenviar la del cliente).

## 6. Lista para el paralelo

| Requisito | Estado |
|---|---|
| Cada esqueleto trabaja solo en `backend/<servicio>/` y `infra/config-repo/<servicio>*.yml` | ✅ ya existen su módulo en el POM padre, su `Dockerfile` y su entrada en el compose |
| No necesitan tocar el POM padre | ⚠️ tras H1 |
| Tienen ejemplos reales | ✅ autoconfiguraciones de `fitstore-common` y `OutboxPublisherTest` (Testcontainers con `@ServiceConnection`) |
| Brief de 0a.2 | ✅ completado con la lectura (spec + micro), lo que construye, la tabla de validación, el núcleo, el detalle por servicio y las lecciones de 0a.1 |

## 7. Observaciones (no bloquean)

- **Memoria del gateway:** ≈ 237 MiB de 256 MiB con los flags de JVM del Dockerfile. Se vigila en 0a.3 con el compose completo y en la spec 8 (si hace falta, se sube su `mem_limit`, que tiene margen dentro de los 6,5 GB).
- **Purga del outbox a los 7 días** (micro de auth 3.1): no es de 0a; se asigna en el brief de la spec 1.
- **Compose completo:** no queda sano hasta que existan los 7 esqueletos (el gateway depende de ellos, orden de 6.2). Lo verifica 0a.3.

## 8. Correcciones

| # | Hallazgo | Corrección | Dónde |
|---|---|---|---|
| H1 | MapStruct y procesadores fuera del POM padre | Agregar al `dependencyManagement` y al `pluginManagement`, con prueba | `fix/ck1-base` (agente de la base) |
| H2 | `SeguridadJwtConfig` fuera de la cobertura | Mover a `com.fitstore.gateway.seguridad` | `fix/ck1-base` |
| H3 | Javadoc de `RateLimitingFilter` | Actualizar | `fix/ck1-base` |
| H4 | Micro: "genera o propaga" | Micro de infraestructura y C3 aclarados | Este PR |
| H5 | Sección 8 de la SPEC-0a sin `ubigeo-a-sql` ni `.dockerignore` | SPEC-0a v7 | Este PR |

**Decisión del autor:** aprobar el CK1 para lanzar 0a.2 cuando `fix/ck1-base` esté verificado e integrado.
