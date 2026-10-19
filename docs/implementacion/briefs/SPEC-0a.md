# Brief — SPEC-0a Plataforma backend y esqueletos

> Preparado por el lead · 2026-10-19 · Spec: [SPEC-0a](../../specs/SPEC-0a-plataforma-backend.md) **v6** · Modelo de los implementadores: Sonnet
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

La SPEC-0a se construye en tres pasos (I1). Cada paso es un agente, una rama y un PR a `dev`. Este brief detalla **0a.1**; los de 0a.2 y 0a.3 se completan cuando 0a.1 esté integrada (se apoyan en lo que 0a.1 deje construido).

| Paso | Rama | Alcance (V4) | Criterios | Entra |
|---|---|---|---|---|
| **0a.1** Monorepo, común e infraestructura | `spec/0a-1-plataforma` | [`SPEC-0a.1.alcance`](SPEC-0a.1.alcance) | 01 (parcial), 04, 08–12, 13 (`X-Request-Id`), 14–17, 21 | En curso |
| **CK1** Checkpoint de base | — | — | Revisión exhaustiva del lead ([estrategia 2.1](../estrategia-multiagente.md#21-checkpoints-de-base)) | 0a.1 en `dev` |
| **0a.2** Esqueleto de un servicio (×7, 3 a la vez) | `spec/0a-2-<servicio>` | `SPEC-0a.2-<servicio>.alcance` | 05 y 06 de ese servicio, más su `ContratoOpenApiTest` (I8) | CK1 aprobado por el autor |
| **0a.3** CI, humo y Postman | `spec/0a-3-ci` | `SPEC-0a.3.alcance` | 01, 02, 03, 07, 13 (traza en Jaeger), 17 (en el compose), 18, 19, 20, 22 | Los 7 esqueletos en `dev` |

---

## 0a.1 — Monorepo, `fitstore-common` e infraestructura

### 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-0a](../../specs/SPEC-0a-plataforma-backend.md) completa, en especial la sección 3 (criterios), la 5 (decisiones I1–I8), la 7 (pruebas y núcleo) y la 8 (archivos).
3. [`infraestructura.md`](../../analisis/micro/infraestructura.md) completo: secciones 2–4 (config, Eureka, gateway), 6 (compose), 7 y 10.3 (`fitstore-common`), 8 (RabbitMQ), 9 (pruebas) y 10 (diseño técnico).
4. [`stack-versiones.md`](../../arquitectura/stack-versiones.md): todas las versiones salen de ahí (Spring Boot 4.0.8, Spring Cloud 2025.1.3, Java 21, Testcontainers 2.0.5, WireMock 3.13.x, JaCoCo 0.8.15). Ojo con Jackson 3 (`tools.jackson`) y los módulos renombrados de Testcontainers 2.
5. [`contracts/README.md`](../../../contracts/README.md) K5, K6, K9 y K12 (formato de `ApiResponse`, `ApiError`, página, cabeceras y sobre de eventos) y [`contracts/openapi/comun.yaml`](../../../contracts/openapi/comun.yaml).
6. Las ADR que cita la sección 2 de la spec, si una decisión no te queda clara.

En tu **primer mensaje**, antes de escribir código: confirma qué leíste y resume en 5–10 líneas lo que vas a construir y en qué orden.

### 2. Qué construyes

1. **Monorepo:** POM padre (Boot 4.0.8 + BOM Spring Cloud 2025.1.3 + `testcontainers-bom` 2.0.5 + JaCoCo 0.8.15 con `check` de 0,80 de líneas por módulo y las exclusiones de I7), Maven Wrapper (3.9.x), `.gitignore` (incluye `.env`, `target/`, `node_modules/`), `.editorconfig`, `README.md` con cómo levantar la infraestructura (sin atribución de IA).
2. **`backend/fitstore-common`** completo según infraestructura 7 y 10.3 (salvo `AzureBlobFileStorage`, diferido).
3. **`backend/config-server`, `backend/discovery-server`, `backend/api-gateway`** completos según infraestructura 2–4 y 10.1, 10.4, 10.5: rutas a los 7 servicios (tabla 4.2), validación de JWT con el JWKS de prueba (I2), bloqueo de `/internal/**`, rate limiting con Redis y paso libre si Redis cae (4.4), CORS, timeouts y fallback `503` (4.5), `X-Request-Id`, Swagger agregado (4.6).
4. **Base mínima de los 7 servicios de negocio** (para que el build y el compose ya los conozcan y en 0a.2 cada agente solo complete su carpeta): `backend/<servicio>/pom.xml` con el padre y `spring-boot-starter-actuator`, la clase `Application` en `com.fitstore.<servicio>`, una prueba de arranque mínima, y el `Dockerfile` de infraestructura 10.6. Nada de entidades, migraciones ni lógica: eso es 0a.2.
5. **`infra/`** para dev: `docker-compose.yml` y `docker-compose.dev.yml` con todos los contenedores de 6.1 (incluidos los 7 servicios), `mem_limit`, versiones fijas de imagen (no `latest`), *healthchecks* y orden de arranque de 6.2; `.env.example` (10.5, sin valores reales); `postgres/init.sql` (7 bases, 7 usuarios con permisos solo sobre la suya); `garage/` (`garage.toml` y el contenedor `garage-init` de I5); `dev/jwks-prueba.json` y su clave privada **de prueba** (I2, marcada como tal en el nombre y en un comentario).
6. **`infra/config-repo/`:** `application.yml` común (10.5), `config-server.yml` si hace falta, `discovery-server.yml` y `api-gateway.yml` (rutas, límites, CORS, timeouts). Los YAML de los 7 servicios **no**: los crea 0a.2.

### 3. Criterios y dónde se prueban

| Criterio | Prueba |
|---|---|
| CA-0a-01 (parcial) | `./mvnw verify` compila los 11 módulos y pasa todo |
| CA-0a-04 | `config-server/src/test`: un cliente con perfil `dev` toma su configuración; sin config-server, reintenta y no arranca con valores por defecto |
| CA-0a-08 a CA-0a-12 | `api-gateway/src/test` con `WebTestClient` + WireMock (+ Redis en Testcontainers para 11) |
| CA-0a-13 (`X-Request-Id`) | `api-gateway/src/test`: la respuesta trae `X-Request-Id` y se propaga al servicio (la traza en Jaeger la verifica 0a.3) |
| CA-0a-17 | `api-gateway/src/test`: el selector de Swagger tiene los 7 servicios configurados (0a.3 lo confirma en el compose) |
| CA-0a-14, CA-0a-15 | `fitstore-common/src/test` con PostgreSQL y RabbitMQ en Testcontainers |
| CA-0a-16 | `fitstore-common/src/test` con Garage en Testcontainers (contenedor genérico) |
| CA-0a-21 | Demostrar que `jacoco:check` falla con un módulo bajo 80 % (por ejemplo, con un módulo de prueba temporal o una configuración de umbral en una prueba del build) y documentar cómo se verificó en el reporte |

Cada prueba lleva el id del criterio en `@DisplayName` (o en un comentario `# CA-0a-xx` en scripts).

**Ajuste tras la fase de lectura (2026-10-19):** CA-0a-07 y CA-0a-18 pasan a 0a.3, porque la sección 7 de la spec los verifica en `smoke.sh` y en `ci.yml`. Se agrega `.dockerignore` al alcance. Decisiones del lead sobre las dudas del agente: `X-Request-Id` con la mayor precedencia (K9), JWKS de prueba leído desde archivo solo en dev (I2), routing key resuelta por un puerto `tipo → routing key` que implementa cada servicio en 0a.2 con los canales del AsyncAPI, compose con el orden exacto de 6.2.

### 4. Núcleo a cubrir por completo

`fitstore-common`: `OutboxPublisher` (solo marca publicado tras la confirmación; pendiente si RabbitMQ cae), `ConsumidorIdempotente` (duplicado ignorado, DLQ tras 3 fallos), `TopologiaRabbit`, `S3FileStorage` (guardar, leer y eliminar en los dos buckets; público sin credenciales), `GlobalExceptionHandlerBase` y `ApiResponse`/`ApiError` (formato K5), configuración de *resource server* y `RoleHierarchy`. Gateway: cada filtro de la cadena de 10.1 y el `FallbackController`.

### 5. Orden sugerido (una historia a la vez, rojo → verde)

1. POM padre, wrapper, `.gitignore`, `.editorconfig` y módulos vacíos que compilen (incluida la base mínima de los 7 servicios).
2. HU-0a-04 (outbox y consumidor) y HU-0a-05 (archivos) en `fitstore-common`, junto con `api` y `security`.
3. `config-server` y `discovery-server` (CA-0a-04).
4. HU-0a-03 en `api-gateway` (CA-0a-08 a CA-0a-13).
5. `infra/`: `init.sql` (7 usuarios aislados; su verificación CA-0a-07 es del *smoke* de 0a.3), compose, Garage, `.env.example`, JWKS de prueba.
6. CA-0a-21 (JaCoCo) y CA-0a-01 con todo el repo.

Antes de pedir verificación: levanta la **infraestructura** del compose (`postgres`, `redis`, `rabbitmq`, `garage`, `jaeger`, `mailpit`, `config-server`, `discovery-server`, `api-gateway`) y comprueba que queda *healthy*. Los 7 servicios todavía no tienen base ni configuración, así que no se exige que arranquen.

### 6. Fuera de alcance de 0a.1

Entidades, migraciones, repositorios y eventos de los servicios (0a.2); `config-repo/<servicio>*.yml` (0a.2); `smoke.sh`, workflows, plantilla de PR, `scripts/contrato.sh`, `contracts/implementados.txt` y Postman (0a.3); cualquier archivo de `docs/` o `contracts/`.

### 7. Detenerse y preguntar

Además de lo de `CLAUDE.md` sección 4: si una versión de imagen Docker no está fijada en `stack-versiones.md`, propón la versión estable más reciente con al menos dos semanas de publicada y pregunta antes de fijarla.
