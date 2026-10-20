# Brief — SPEC-0a Plataforma backend y esqueletos

> Preparado por el lead · 2026-10-19 · Spec: [SPEC-0a](../../specs/SPEC-0a-plataforma-backend.md) **v6** · Modelo de los implementadores: Sonnet
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

La SPEC-0a se construye en tres pasos (I1). Cada paso es un agente, una rama y un PR a `dev`. Este brief detalla **0a.1**; los de 0a.2 y 0a.3 se completan cuando 0a.1 esté integrada (se apoyan en lo que 0a.1 deje construido).

| Paso | Rama | Alcance (V4) | Criterios | Entra |
|---|---|---|---|---|
| **0a.1** Monorepo, común e infraestructura | `spec/0a-1-plataforma` | [`SPEC-0a.1.alcance`](SPEC-0a.1.alcance) | 01 (parcial), 04, 08–13, 14–16, 21 · complementaria: 17 | En curso |
| **CK1** Checkpoint de base | — | — | Revisión exhaustiva del lead ([estrategia 2.1](../estrategia-multiagente.md#21-checkpoints-de-base)) | 0a.1 en `dev` |
| **0a.2** Esqueleto de un servicio (×7, 3 a la vez) | `spec/0a-2-<servicio>` | `SPEC-0a.2-<servicio>.alcance` | 06 de ese servicio, más su `ContratoOpenApiTest` (I8) y lo que necesita 05 (V1 + entidades con `validate`) | CK1 aprobado por el autor |
| **0a.3** CI, humo y Postman | `spec/0a-3-ci` | `SPEC-0a.3.alcance` | 01, 02, 03, 05, 07, 17, 18, 19, 20, 22 | Los 7 esqueletos en `dev` |

### Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-0a v6. Cada criterio tiene un solo dueño.

| CA | Sección 7: tipo · dónde | Dueño | ¿Su archivo está en el alcance del dueño? |
|---|---|---|---|
| 01 | Build / CI · `./mvnw verify`, `ci.yml` | 0a.3 (0a.1 deja el build en verde) | ✅ `ci.yml` en 0a.3 |
| 02, 03 | Humo · `infra/smoke.sh` | 0a.3 | ✅ |
| 04 | Integración · `config-server` + un servicio con Testcontainers | 0a.1 | ✅ `backend/config-server/` |
| 05 | Humo · `infra/smoke.sh` | 0a.3 (0a.2 construye V1 y entidades) | ✅ |
| 06 | Datos · `src/test/.../repository/*Test.java` de cada servicio | 0a.2 (cada servicio) | ✅ `backend/<servicio>/` |
| 07 | Humo · `infra/smoke.sh` | 0a.3 (0a.1 crea `init.sql`) | ✅ |
| 08–13 | Integración · `api-gateway/src/test` | 0a.1 | ✅ `backend/api-gateway/` |
| 14, 15, 16 | Integración · `fitstore-common/src/test` | 0a.1 | ✅ `backend/fitstore-common/` |
| 17 | Humo · `infra/smoke.sh` | 0a.3 (0a.1: prueba complementaria) | ✅ |
| 18 | CI · `.gitignore` + gitleaks en `ci.yml` | 0a.3 (0a.1 crea `.gitignore`) | ✅ |
| 19 | Humo · `infra/smoke.sh` | 0a.3 | ✅ |
| 20 | CI · `ci.yml` | 0a.3 | ✅ |
| 21 | Build · `jacoco:check` en `./mvnw verify` | 0a.1 (POM padre) | ✅ `pom.xml` |
| 22 | CI · `ci.yml`, `scripts/contrato.sh`; `ContratoOpenApiTest` en cada esqueleto | 0a.3 (0a.2: `ContratoOpenApiTest`) | ✅ |

Errores encontrados y corregidos (2026-10-19): CA-0a-07 y 18 estaban en 0a.1 (son de 0a.3); CA-0a-05 estaba en 0a.2 y CA-0a-17 en 0a.1 (los dos son *smoke*, de 0a.3); la traza de CA-0a-13 estaba en 0a.3 (la sección 7 la pone en las pruebas del gateway, 0a.1).

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
| CA-0a-13 | `api-gateway/src/test`: la respuesta trae `X-Request-Id`, se propaga al servicio y el gateway exporta la traza (span del gateway con el servicio destino) a un receptor OTLP de prueba; la sección 7 de la spec pone todo el 13 en las pruebas del gateway |
| CA-0a-17 (complementaria) | `api-gateway/src/test`: el selector de Swagger tiene los 7 servicios configurados. El dueño del criterio es 0a.3 (*smoke*, sección 7) |
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

---

## 0a.2 — Esqueleto de un servicio (×7, 3 a la vez)

> Completado tras el checkpoint CK1 (2026-10-19). Un agente por servicio, cada uno con su rama `spec/0a-2-<servicio>` desde `dev` y su archivo de alcance `SPEC-0a.2-<servicio>.alcance`. Entra cuando el autor aprueba el CK1 y está integrada la corrección `fix/ck1-base` (MapStruct y procesadores en el POM padre).

**Orden de lanzamiento (3 a la vez):** auth, catalog y settings → order, payment y delivery → notification (entra cuando se libera un cupo).

### 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-0a](../../specs/SPEC-0a-plataforma-backend.md): sección 1 (punto "Esqueleto de cada servicio de negocio"), 3 (CA-0a-05 y CA-0a-06), 5 (I3, I4, I6, I8), 6 (datos) y 7 (tabla de repositorios de tu servicio).
3. **El micro de tu servicio** (`docs/analisis/micro/<servicio>-service.md`): secciones **3** (modelo de datos completo: tablas, índices, `CHECK`, secuencias, semillas), **6** (eventos que publica y consume), **9** (pruebas de capa de datos: la tabla de tu servicio) y **10** (10.4 dependencias, 10.5 configuración y puerto, 10.6 infraestructura).
4. [`infraestructura.md`](../../analisis/micro/infraestructura.md) sección 7, 8 y 10.3: qué te da `fitstore-common` y cómo se activa.
5. [`contracts/asyncapi/fitstore-eventos.yaml`](../../../contracts/asyncapi/fitstore-eventos.yaml): los canales (routing keys) de los eventos que publica tu servicio.
6. [`stack-versiones.md`](../../arquitectura/stack-versiones.md) v5.
7. **El código de la base ya integrada**, como ejemplo de estilo y de pruebas: `backend/fitstore-common` (autoconfiguraciones y propiedades `fitstore.*`), `fitstore-common/src/test/.../event/OutboxPublisherTest.java` (Testcontainers con `@ServiceConnection`) y la base mínima de tu servicio (`pom.xml`, `Application`, `Dockerfile`).

En tu primer mensaje: qué leíste, las tablas, entidades, repositorios y eventos que vas a crear, y cómo probarás cada repositorio.

### 2. Qué construyes (solo en tu carpeta)

1. **`pom.xml` de tu servicio:** las dependencias de la sección 10.4 de tu micro (versiones del POM padre; no toques el POM padre), más `spring-cloud-starter-config` con `spring-retry` (CA-0a-04), Flyway, `fitstore-common`, `springdoc-openapi-starter-webmvc-ui` y Testcontainers. Lombok y MapStruct, si los usas, ya están configurados en el POM padre.
2. **`infra/config-repo/<servicio>.yml`, `<servicio>-dev.yml` y `<servicio>-cloud.yml`** según la sección 10.5 de tu micro (puerto, datasource con las variables de `.env.example`, `fitstore.eventos.*`, `fitstore.outbox.*`, `fitstore.storage.*` si aplica, `springdoc.api-docs.version=openapi_3_0`). Sin secretos: solo `${VARIABLES}`.
3. **Migraciones Flyway** en `src/main/resources/db/migration/`: `V1__esquema_inicial.sql` con **todo** el esquema de la sección 3.1 (incluidas `outbox_evento` y `evento_procesado` con los tipos de infraestructura 10.3, si tu servicio publica o consume eventos) y las semillas aprobadas según SPEC-0a I6 y tu micro (`V2__…`, `V3__…` cuando el micro las nombra así).
4. **Entidades JPA, enums y repositorios** de la sección 3, con `spring.jpa.hibernate.ddl-auto=validate` (lo que necesita CA-0a-05).
5. **Pruebas de capa de datos (CA-0a-06)** de la tabla de la sección 7 de la spec (y la sección 9 de tu micro): `@DataJpaTest` + PostgreSQL en Testcontainers (Redis para el carrito de order), con insertar, actualizar, eliminar (físico o lógico según el micro), listar y las restricciones (duplicados, `CHECK`, índices únicos, concurrencia cuando la tabla lo pide). `@DisplayName("CA-0a-06 …")`.
6. **Eventos (I4):** las clases de los eventos que publica y consume (sección 6) como `record`; la implementación de `CatalogoRutasEventos` con las routing keys de los canales del AsyncAPI (prueba que cada evento que publica tu servicio tiene su routing key); `fitstore.eventos.enlaces` con las routing keys que consume.
7. **`ContratoOpenApiTest` (I8):** `@SpringBootTest` + MockMvc que guarda `GET /v3/api-docs` en `target/openapi.json` (todavía casi vacío: los endpoints llegan con cada spec). Con `@DisplayName("CA-0a-22 …")`.
8. **Seguridad base (I3):** la de `fitstore-common`, sin abrir endpoints de negocio. `/actuator/health` y la documentación quedan abiertos.

### 3. Criterios (tabla de validación)

| CA | Qué te toca | Prueba |
|---|---|---|
| CA-0a-06 | Todos los repositorios de tu servicio de la tabla de la sección 7 | `src/test/.../repository/*Test.java` |
| CA-0a-05 | Lo que lo hace posible: `V1` (y semillas) + entidades con `validate`. Su verificación es del *smoke* de 0a.3 | Tus pruebas `@DataJpaTest` ya corren Flyway + `validate` |
| CA-0a-22 | Solo `ContratoOpenApiTest` | `src/test/.../ContratoOpenApiTest.java` |

### 4. Núcleo a cubrir por completo

Los repositorios con reglas: consultas propias, restricciones y concurrencia (reserva atómica de stock en catalog, correlativo de reclamos en settings, búsqueda jerárquica de tarifas en delivery) y la implementación de `CatalogoRutasEventos`.

### 5. Por servicio

| Servicio | Puerto | Migraciones y semillas | Repositorios (CA-0a-06) | Extra |
|---|---|---|---|---|
| auth | 8081 | `V1` | `ClienteRepository`, `StaffRepository`, `DireccionRepository`, `TokenPasswordRepository` | — |
| catalog | 8082 | `V1` + semillas de tallas, colores y política de descuentos | `CategoriaRepository`, `ProductoRepository`, `VarianteRepository`, `StockRepository` (reserva atómica + concurrencia), `MovimientoStockRepository`, `TramoDescuentoRepository` | — |
| settings | 8087 | `V1`, `V2__ubigeo_inei.sql`, `V3__configuracion_semilla.sql` | `ConfiguracionTiendaRepository`, `ImagenTiendaRepository`, `PoliticaRepository`, `ReclamoRepository` (+ correlativo concurrente), `UbigeoRepository` | `infra/scripts/ubigeo-a-sql` (SPEC-0a sección 6): genera `V2` desde el CSV oficial del INEI |
| order | 8083 | `V1` | `PedidoRepository`, `CarritoRepository` (Redis en Testcontainers) | — |
| payment | 8084 | `V1` | `IntencionPagoRepository`, `TransaccionRepository`, `ReembolsoRepository`, `BoletaRepository` | — |
| delivery | 8085 | `V1`, `V2__semilla.sql` (agencias y tarifas) | `RepartidorRepository`, `TarifaEnvioRepository` (búsqueda jerárquica), `AgenciaRepository`, `EntregaRepository` | — |
| notification | 8086 | `V1` + semilla de plantillas | `EnvioCorreoRepository`, `PedidoResumenRepository`, `MarcaTiendaRepository`, `PlantillaRepository` | — |

### 6. Fuera de alcance de 0a.2

Controladores, servicios de aplicación, lógica de negocio, consumidores con lógica y pantallas (specs 1–7); `smoke.sh`, CI y Postman (0a.3); el POM padre, `fitstore-common`, el gateway, `infra/docker-compose*.yml`, `infra/config-repo/application.yml` (congelados); los datos de demostración (`db/dev`, los agrega cada spec).

### 7. Lecciones de 0a.1 (CK1)

- Hibernate 7.2 + Jackson 3: las columnas `jsonb` ya funcionan con el `JacksonJsonFormatMapper` de `fitstore-common`; no agregues otro mapper.
- El SDK de S3 con Garage necesita checksums `WHEN_REQUIRED` (ya lo hace `S3FileStorage`).
- Testcontainers 2: módulos `testcontainers-postgresql`, `-rabbitmq`, `-junit-jupiter`; usa `@ServiceConnection` como en `OutboxPublisherTest`.
- Si el puerto 5432 de la máquina está ocupado, las pruebas no se afectan (Testcontainers usa puertos aleatorios).

---

## 0a.3 — CI, humo y Postman

> Completado tras integrar los 7 esqueletos (2026-10-20, PRs #20 a #26). Un agente, rama `spec/0a-3-ci` desde `dev`, alcance [`SPEC-0a.3.alcance`](SPEC-0a.3.alcance). Cierra la SPEC-0a.

### 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-0a](../../specs/SPEC-0a-plataforma-backend.md) completa, en especial la sección 1 ("Incluye": CI y colección Postman base), 3 (todos los criterios), 5 (I7, I8), 7 (tabla de pruebas y convención de nombres), 8 y 9 (DoD).
3. [ADR-021](../../adr/0021-flujo-de-ramas-dev-stage-main.md) (flujo de ramas y qué hace cada workflow) y [ADR-018](../../adr/0018-monorepo-build-y-ci-con-github-actions.md).
4. [`infraestructura.md`](../../analisis/micro/infraestructura.md) secciones 6 (compose, memoria y orden de arranque), 9 (pruebas: *smoke*) y 10.
5. [`contracts/README.md`](../../../contracts/README.md): K2 (`oasdiff`) y la tabla de herramientas (Redocly CLI 2.57.0, AsyncAPI CLI 6.2.0, `tufin/oasdiff:v1.33.0`).
6. [`stack-versiones.md`](../../arquitectura/stack-versiones.md) v5.
7. El estado real del repo: `pom.xml`, `infra/` (compose, `.env.example`, `init.sql`, `dev/`), `infra/config-repo/` y un `ContratoOpenApiTest` de cualquier servicio (genera `target/openapi.json`).

En tu primer mensaje: qué leíste, qué vas a construir y cómo verificarás cada criterio.

### 2. Qué construyes

1. **`infra/smoke.sh`**: levanta el compose completo de dev (`docker compose … up -d --wait`), espera a que todo quede *healthy* y verifica los criterios de humo; termina con código 0 solo si todo pasa. Cada verificación lleva un comentario `# CA-0a-xx`. Debe poder correr en Lyanna y en el runner de GitHub Actions.
2. **`.github/workflows/ci.yml`** (PR y push a `dev`): compilación y pruebas con `./mvnw verify` (incluye JaCoCo), con **filtros por ruta** (solo se compila lo que cambió más `fitstore-common`) y caché de Maven; **gitleaks**; **Redocly** sobre `contracts/openapi/*-service.yaml` y **AsyncAPI CLI** sobre `fitstore-eventos.yaml`; **`oasdiff`** para cada servicio listado en `contracts/implementados.txt`. Cada paso con su comentario `# CA-0a-xx`.
3. **`.github/workflows/stage.yml`** (PR y push a `stage`, PR a `main`): construye las imágenes, levanta el compose completo en el runner, corre `infra/smoke.sh` y la colección Postman con Newman.
4. **`.github/pull_request_template.md`**: la lista de verificación del PR (V1 a V11 de la estrategia, en corto).
5. **`scripts/contrato.sh <servicio>`**: genera `target/openapi.json` con el `ContratoOpenApiTest` del servicio y corre `oasdiff breaking contracts/openapi/<servicio>.yaml target/openapi.json` con la imagen `tufin/oasdiff:v1.33.0`.
6. **`contracts/implementados.txt`**: vacío al cerrar 0a (con un comentario que explique su uso); cada spec agrega su servicio al terminar.
7. **`.gitleaks.toml`**: configuración de gitleaks con la *allowlist* de la clave de prueba `infra/dev/clave-privada-PRUEBA-solo-dev.jwk.json` (y el JWKS público de prueba), y nada más.
8. **Postman**: `postman/fitstore-base.postman_collection.json` y `postman/local.postman_environment.json` con el *health* de la plataforma **a través del gateway** (ver decisión L1 abajo).
9. **`README.md`**: completar la sección de cómo levantar el entorno dev completo y correr el humo (DoD de la spec; sin atribución de IA).

### 3. Criterios (tabla de validación)

| CA | Qué verifica | Dónde |
|---|---|---|
| CA-0a-01 | `./mvnw verify` compila los 11 módulos y pasa todo | `ci.yml` (y en local) |
| CA-0a-02 | Arranque completo: todo *healthy* en menos de 5 minutos y `smoke.sh` termina con 0 | `smoke.sh` |
| CA-0a-03 | Eureka lista `api-gateway` y los 7 servicios con estado UP | `smoke.sh` |
| CA-0a-05 | Cada servicio aplicó `V1` (y sus semillas) en su base, con Hibernate en `validate` | `smoke.sh` (consulta `flyway_schema_history` de cada base y el arranque sano) |
| CA-0a-07 | El usuario de un servicio no puede leer la base de otro | `smoke.sh` (con `psql` en el contenedor de PostgreSQL) |
| CA-0a-17 | `/swagger-ui` del gateway ofrece los 7 servicios y cada documentación responde | `smoke.sh` |
| CA-0a-18 | Sin secretos versionados; `.env` ignorado y solo `.env.example` | `.gitignore` + gitleaks en `ci.yml` |
| CA-0a-19 | Cada contenedor tiene `mem_limit` y la suma no supera 6,5 GB | `smoke.sh` (`docker inspect` / `docker stats`) |
| CA-0a-20 | Un PR que solo toca `backend/catalog-service` compila y prueba `fitstore-common` y catalog, con caché | `ci.yml` (filtros por ruta) |
| CA-0a-22 | Redocly + AsyncAPI CLI en cada PR; `oasdiff` para los servicios de `implementados.txt` | `ci.yml` + `scripts/contrato.sh` |

Los demás criterios ya tienen dueño y prueba (0a.1 y 0a.2). `scripts/ca-cobertura.sh 0a` debe quedar en **22 de 22**.

### 4. Núcleo

`smoke.sh` (cada verificación debe fallar de verdad si la condición no se cumple: pruébalo rompiéndola a propósito al menos una vez por criterio y deja constancia en el reporte) y los filtros por ruta de `ci.yml`.

### 5. Decisiones del lead

- **L1 — Postman "health a través del gateway":** el gateway no enruta `/actuator/**` de los servicios (su configuración está congelada). La colección base verifica `GET /actuator/health` del gateway y, por cada uno de los 7 servicios, `GET /<servicio>/v3/api-docs` a través del gateway (200): prueba que el servicio está arriba, registrado y enrutado.
- **L2 — Versiones no fijadas en stack-versiones** (gitleaks, Newman, *actions* de GitHub): propón la versión estable más reciente con al menos dos semanas de publicada y pregúntame antes de fijarla.
- **L3 — Si el compose completo no queda sano** por un problema dentro de un servicio (su YAML o su código), **no lo corrijas tú**: detente y repórtalo con el diagnóstico (log, contenedor, causa). El lead decide la corrección en un PR aparte.
- **L4 — Memoria:** el gateway usa ≈ 237 MiB de 256 MiB (CK1). Si con el compose completo algún contenedor se reinicia por memoria, repórtalo; no cambies `mem_limit` sin aprobación.

### 6. Fuera de alcance

Código de los servicios, `fitstore-common`, el gateway, el POM padre, `infra/docker-compose*.yml` e `infra/config-repo/` (congelados o de sus specs); `deploy.yml` (spec 8); el job de frontend de `ci.yml` (0b); cualquier archivo de `docs/` o `contracts/` salvo `contracts/implementados.txt`.
