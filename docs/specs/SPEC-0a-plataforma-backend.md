# SPEC-0a — Plataforma backend y esqueletos de servicios

> Versión: **v9 (aprobado)** — 2026-10-19 · Estados: ✅ decidido · 🟡 propuesto · ❓ abierto
> **Dueña de:** monorepo, `fitstore-common`, `config-server`, `discovery-server`, `api-gateway`, `infra/` (entorno dev), CI, y el **esqueleto** de los 7 servicios de negocio.
> **Depende de:** — · **Habilita:** todas las demás specs.
> **Contratos:** los esqueletos exponen solo `actuator` y la documentación base; los endpoints de negocio llegan con cada spec.

**Historial:**
- v9 (2026-10-20): **spec implementada** (0a.1 #17, 0a.2 #20–#26, 0a.3 #29; 22 de 22 criterios). La sección 8 incluye `.gitleaksignore` (los 10 falsos positivos de gitleaks, excluidos por huella exacta; decisión del autor).
- v8 (2026-10-20): brief de 0a.3: la sección 8 incluye `.gitleaks.toml` (allowlist de la clave de prueba de I2, CA-0a-18).
- v7 (2026-10-19): checkpoint CK1: la sección 8 incluye `infra/scripts/ubigeo-a-sql` (ya lo pedía la sección 6) y `.dockerignore`.
- v6 (2026-10-19): por la estrategia multiagente: cobertura mínima con JaCoCo (CA-0a-21), validación de contratos en CI con Redocly, AsyncAPI CLI y `oasdiff` v1.33.0 (CA-0a-22, K2), I1 con integración en `dev` y los esqueletos de 3 en 3, núcleo a cubrir en la sección 7.
- v5 (2026-10-19): por la SPEC-8: Flyway incluye `db/dev` con los perfiles `dev` y `demo` (CA-0a-05); el adaptador Azure Blob queda diferido (Garage en la VM, I6).
- v4 (2026-10-18): CI según la ADR-021 (`ci.yml` + `stage.yml` + plantilla de PR); sin protección de rama.
- v3 (2026-10-18): secciones 2, 3, 4, 6, 7, 8 y 9 aprobadas → **spec aprobada**.
- v2 (2026-10-18): estructura de la plantilla v2 (CA dentro de cada HU). Aprobados: alcance, orden interno (I1), JWKS de prueba (I2), seguridad base (I3), Garage (I5). El repo en GitHub lo crea el asistente (privado). Azure Blob queda para la decisión de despliegue en la nube.
- v1 (2026-10-18): borrador inicial.

**Estado de revisión por sección:** 1 ✅ · 2 ✅ · 3 ✅ · 4 ✅ · 5 ✅ · 6 ✅ · 7 ✅ · 8 ✅ · 9 ✅

---

## 1. Objetivo y alcance

Al cerrar esta spec, en Lyanna se ejecuta `docker compose up` y **toda la plataforma arranca sana**: infraestructura, config-server, Eureka, gateway y los 7 servicios de negocio registrados, con su base migrada y **sus pruebas de capa de datos en verde** (insertar, actualizar, eliminar y listar → **AP1, 6 pts**). Ningún servicio tiene todavía lógica de negocio.

**Incluye:**
- Monorepo con POM padre, Maven Wrapper y estructura de carpetas (infraestructura 10.2).
- `fitstore-common` completo (infraestructura 7 y 10.3), salvo el adaptador de Azure Blob.
- `config-server`, `discovery-server` y `api-gateway` completos (infraestructura 2–4): rutas a los 7 servicios, validación de JWT, rate limiting, CORS, bloqueo de `/internal`, fallback `503`, `X-Request-Id`, Swagger agregado.
- `infra/` para **dev**: `docker-compose.yml` + `docker-compose.dev.yml`, `postgres/init.sql` (7 bases y 7 usuarios), configuración de Garage y creación de buckets, `.env.example`, `smoke.sh`, `config-repo/` (común + un archivo por servicio).
- **Esqueleto de cada servicio de negocio** (auth, catalog, settings, order, payment, delivery, notification):
  - módulo Maven, clase `Application`, dependencias de su sección 10.4;
  - su YAML en `config-repo` (sección 10.5) y su `Dockerfile`;
  - migración **`V1__esquema_inicial.sql` con todo el esquema** de su sección 3 (más semillas aprobadas: ubigeo, plantillas, agencias y tarifas, configuración inicial, tallas y colores, política de descuentos);
  - **entidades JPA, enums y repositorios** de la sección 3, con `ddl-auto=validate`;
  - **pruebas de capa de datos** de su sección 9 (`@DataJpaTest` + Testcontainers);
  - clases de los eventos que publica y consume (sección 6) como `record`;
  - seguridad base (*resource server* de `fitstore-common`), `actuator/health`, OpenAPI base.
- Imagen Docker de cada servicio (Dockerfile común de infraestructura 10.6).
- CI en GitHub Actions según la ADR-021: `ci.yml` (PR y push a `dev`: compilación y pruebas con filtros por ruta y caché, cobertura con JaCoCo, gitleaks, validación de contratos) y `stage.yml` (PR y push a `stage`/PR a `main`: imágenes, compose en el runner, `smoke.sh`, Postman con Newman). Plantilla de PR con la lista de verificación.
- Colección Postman base: *health* de todos los servicios a través del gateway.

**No incluye (y dónde va):**
- Lógica de negocio, controladores y pantallas → specs 1–7.
- Workspace Angular y librería `shared` → 0b.
- Emisión de JWT (auth firma) → spec 1. En 0a, el gateway y los servicios **validan** con un JWKS de prueba.
- `docker-compose.cloud.yml`, Caddy, DuckDNS, SES y el perfil `demo` de la nube → spec 8. Adaptador Azure Blob → diferido: en la nube se usa Garage en la VM (SPEC-8 I6).

## 2. Referencias

- Macro: ADR-001, 002, 006, 007, 008, 009 (outbox), 010, 011, 013, 015, 017, 018, 019, 020; RNF-03, 04, 07, 08, 11, 12, 14, 15, 16.
- Micro: [`infraestructura.md`](../analisis/micro/infraestructura.md) completo; sección 3 (datos), 6 (eventos), 9 (pruebas de datos) y 10 (diseño técnico) de los 7 micros de negocio.
- [`stack-versiones.md`](../arquitectura/stack-versiones.md) (versiones exactas).

## 3. Historias de usuario y criterios de aceptación

Esta spec no tiene pantallas, así que las historias no citan bocetos.

### HU-0a-01 — Levantar la plataforma con un comando

Como desarrollador, quiero levantar toda la plataforma con un comando, para trabajar y probar en local. · Cubre: ADR-002, RNF-03, RNF-08, RNF-16 · Pantallas: no aplica

```gherkin
CA-0a-02  Arranque completo
          Dado un .env creado desde .env.example
          Cuando ejecuto docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d
          Entonces en menos de 5 minutos todos los contenedores están "healthy" e infra/smoke.sh termina con código 0

CA-0a-03  Registro en Eureka
          Dado la plataforma levantada
          Cuando consulto el registro de Eureka
          Entonces aparecen api-gateway y los 7 servicios de negocio con estado UP

CA-0a-04  Configuración centralizada
          Dado config-server levantado
          Cuando un servicio arranca con el perfil dev
          Entonces toma su configuración de config-server
          Y si config-server no responde, reintenta y no arranca con valores por defecto

CA-0a-07  Aislamiento de datos
          Dado los 7 usuarios de base de datos
          Cuando el usuario de un servicio intenta leer la base de otro
          Entonces PostgreSQL lo rechaza

CA-0a-19  Memoria
          Dado la plataforma levantada en reposo
          Cuando reviso docker stats
          Entonces cada contenedor tiene mem_limit y la suma no supera 6,5 GB (RNF-03)
```

### HU-0a-02 — Esquema y repositorios listos y probados

Como desarrollador, quiero que cada servicio tenga su esquema y sus repositorios listos y probados, para que las specs de negocio solo agreguen lógica. · Cubre: ADR-006, ADR-007, RNF-12 · **AP1 (6 pts)** · Pantallas: no aplica

```gherkin
CA-0a-05  Esquema migrado y validado
          Dado un PostgreSQL vacío
          Cuando arranca cada servicio
          Entonces Flyway aplica V1 en su base
          Y Hibernate valida (ddl-auto=validate) que las entidades coinciden con el esquema
          Y solo con los perfiles dev o demo Flyway incluye además classpath:db/dev (datos de demostración de cada spec)

CA-0a-06  Pruebas de capa de datos (AP1)
          Dado cada repositorio listado en la sección 9 de su micro
          Cuando ejecuto sus pruebas @DataJpaTest con PostgreSQL en Testcontainers (Redis para el carrito)
          Entonces pasan los casos de insertar, actualizar, eliminar (físico o lógico según el micro) y listar,
               y los casos de restricción indicados (duplicados, CHECK, índices únicos)
```

### HU-0a-03 — Gateway que enruta, protege y limita

Como desarrollador, quiero un gateway que enrute, proteja y limite las peticiones, para que ningún servicio quede expuesto directamente. · Cubre: ADR-011, ADR-020, RNF-05, RNF-07, RNF-14, RNF-15 · Pantallas: no aplica

```gherkin
CA-0a-08  Enrutamiento del gateway
          Dado un servicio de negocio registrado
          Cuando llamo GET /api/v1/<prefijo de ese servicio>/… a través del gateway
          Entonces la petición llega a ese servicio (según la tabla de infraestructura 4.2)

CA-0a-09  Rutas internas bloqueadas
          Cuando llamo cualquier /internal/** a través del gateway
          Entonces recibo 404

CA-0a-10  JWT inválido
          Dado un token con firma inválida o vencido
          Cuando llamo una ruta protegida a través del gateway
          Entonces recibo 401 con formato ApiResponse y la petición no llega al servicio

CA-0a-11  Rate limiting
          Cuando hago 6 POST /api/v1/auth/login desde la misma IP en un minuto
          Entonces el sexto recibe 429 con cabecera Retry-After
          Y si Redis no está disponible, el gateway deja pasar y registra una advertencia

CA-0a-12  Servicio caído
          Dado un servicio de negocio detenido
          Cuando llamo una de sus rutas a través del gateway
          Entonces recibo 503 con mensaje "Servicio no disponible, inténtalo en un momento"

CA-0a-13  Correlación y trazas
          Cuando hago una petición a través del gateway
          Entonces la respuesta trae X-Request-Id
          Y en Jaeger aparece una traza con el gateway y el servicio destino

CA-0a-17  Swagger agregado
          Cuando abro /swagger-ui en el gateway
          Entonces veo un selector con los 7 servicios
```

### HU-0a-04 — Eventos confiables sin repetir código

Como desarrollador, quiero publicar y consumir eventos de forma confiable sin escribir esa lógica en cada servicio. · Cubre: ADR-008, ADR-009, RNF-09 · Pantallas: no aplica

```gherkin
CA-0a-14  Outbox
          Dado un servicio que guarda un evento en outbox_evento dentro de su transacción
          Cuando RabbitMQ confirma la publicación
          Entonces la fila queda con publicado_en
          Y si RabbitMQ no está disponible, la fila queda pendiente y se publica cuando vuelve

CA-0a-15  Consumidor idempotente
          Dado un evento con un eventoId ya procesado
          Cuando llega de nuevo
          Entonces se ignora sin efectos
          Y un mensaje que falla 3 veces termina en la cola .dlq de su servicio
```

### HU-0a-05 — Archivos con un solo puerto

Como desarrollador, quiero guardar y servir archivos con una sola interfaz, para que el almacenamiento sea portable. · Cubre: ADR-013 · Pantallas: no aplica

```gherkin
CA-0a-16  Almacenamiento
          Dado Garage con los buckets fitstore-media (público) y fitstore-privado
          Cuando FileStorage guarda, lee y elimina un archivo
          Entonces funciona en ambos buckets
          Y solo los archivos del bucket público son accesibles sin credenciales
```

### HU-0a-06 — Integración segura de cambios

Como autor, quiero que el CI compile y pruebe cada cambio y que no se filtren secretos, para integrar las ramas de los agentes con seguridad. · Cubre: ADR-018, RNF-04 · Pantallas: no aplica

```gherkin
CA-0a-01  Compilación
          Dado el repositorio recién clonado
          Cuando ejecuto ./mvnw verify
          Entonces compilan los 11 módulos (common, 3 de infraestructura, 7 de negocio) y pasan todas las pruebas

CA-0a-18  Sin secretos
          Dado el repositorio
          Cuando reviso los archivos versionados
          Entonces no hay contraseñas, claves ni tokens reales; .env está en .gitignore y solo existe .env.example

CA-0a-20  CI
          Dado un pull request que solo modifica backend/catalog-service
          Cuando se ejecuta GitHub Actions
          Entonces compila y prueba fitstore-common y catalog-service, usando caché de Maven

CA-0a-21  Cobertura mínima
          Dado un módulo con menos de 80 % de líneas cubiertas (sin contar configuración, DTOs,
               clases Application ni código generado)
          Cuando se ejecuta ./mvnw verify
          Entonces la verificación de JaCoCo falla e indica el módulo y su cobertura

CA-0a-22  Contratos en CI
          Dado un pull request
          Cuando se ejecuta ci.yml
          Entonces Redocly valida los OpenAPI de contracts/ y AsyncAPI CLI valida fitstore-eventos.yaml
          Y para cada servicio listado en contracts/implementados.txt, oasdiff breaking compara su contrato
               con el OpenAPI que genera el servicio y falla si hay un cambio incompatible
```

## 4. Pantallas

No aplica (sin frontend). Las consolas de apoyo en dev: Eureka `:8761`, RabbitMQ `:15672`, Jaeger `:16686`, Mailpit `:8025`, Swagger `:8080/swagger-ui`.

## 5. Backend

Todo está definido en [`infraestructura.md`](../analisis/micro/infraestructura.md) y en la sección 10 de cada micro. Decisiones de implementación propias de esta spec:

| # | Decisión | Detalle |
|---|---|---|
| I1 | **Orden de construcción interno** ✅ | **0a.1** monorepo, `fitstore-common`, infraestructura (compose, config, Eureka, gateway) → **0a.2** los 7 esqueletos, **de 3 en 3** (no comparten archivos; estrategia multiagente O1) → **0a.3** CI, smoke y Postman. 0a.1 deja una base mínima de los 7 servicios (módulo Maven, clase `Application`, `Dockerfile`, entrada en el POM padre y en el compose) para que en 0a.2 cada agente solo toque su carpeta y su YAML. 0a.2 empieza cuando 0a.1 está en `dev`; cada paso entra por su PR |
| I2 | **JWKS de prueba** ✅ | Mientras auth no firma (spec 1), `infra/dev/jwks-prueba.json` y su clave privada de prueba (solo dev, marcada como tal) permiten probar la validación del gateway y de los servicios. Se elimina al cerrar la spec 1 |
| I3 | **Seguridad base de los esqueletos** ✅ | Todo cerrado salvo `/actuator/health` y la documentación OpenAPI; cada spec abre sus endpoints con `@PreAuthorize` |
| I4 | **Topología de RabbitMQ** | La declara cada servicio al arrancar (`TopologiaRabbit` de `fitstore-common`): el exchange `fitstore.eventos`, su cola, sus enlaces y su DLQ, según su sección 10.6 |
| I5 | **Buckets de Garage** ✅ | Un contenedor de inicialización (`garage-init`) crea la clave de acceso y los dos buckets la primera vez, y deja el público con lectura anónima |
| I7 | **Cobertura** ✅ | `jacoco-maven-plugin` 0.8.15 en el POM padre: `report` y `check` en `verify`, mínimo **0,80 de líneas** por módulo; excluye `**/config/**`, `**/dto/**`, `**/*Application.class` y código generado |
| I8 | **OpenAPI generado para `oasdiff`** ✅ | Cada esqueleto trae `ContratoOpenApiTest` (`@SpringBootTest` + MockMvc) que guarda `GET /v3/api-docs` en `target/openapi.json` (springdoc en `openapi_3_0`, K2). `scripts/contrato.sh <servicio>` corre `oasdiff breaking` (v1.33.0, imagen `tufin/oasdiff`) entre `contracts/openapi/<servicio>.yaml` y ese archivo. En CI solo se aplica a los servicios de `contracts/implementados.txt` (vacío al cerrar 0a; cada spec agrega el suyo al terminar), porque antes el contrato tiene endpoints que el código aún no expone |
| I6 | **Semillas en `V1`** | Las tablas maestras aprobadas se siembran en la misma migración o en `V2__semilla.sql` del servicio dueño: ubigeo INEI (settings), plantillas (notification), agencias y tarifas (delivery), tallas, colores y política de descuentos (catalog), configuración inicial de tienda (settings) |

## 6. Datos

- `V1__esquema_inicial.sql` de cada servicio = su sección 3.1 (tablas, índices, `CHECK`, secuencias, `outbox_evento`, `evento_procesado`).
- Semillas: ver I6. El ubigeo se genera desde el CSV oficial del INEI con un script versionado (`infra/scripts/ubigeo-a-sql`), para poder regenerarlo.
- `infra/postgres/init.sql`: 7 bases (`auth_db` … `settings_db`) y 7 usuarios con permisos solo sobre la suya.

## 7. Pruebas

| CA | Tipo | Dónde |
|---|---|---|
| 01, 20 | Build / CI | `./mvnw verify`, `.github/workflows/ci.yml` |
| 02, 03, 05, 07, 17, 19 | Humo (compose) | `infra/smoke.sh` |
| 04 | Integración | `config-server` + un servicio con Testcontainers |
| 06 | Datos (AP1) | `src/test/.../repository/*Test.java` de cada servicio (ver tabla) |
| 08–13 | Integración | `api-gateway/src/test` (WebTestClient + WireMock + Redis Testcontainers) |
| 14, 15 | Integración | `fitstore-common/src/test` (PostgreSQL + RabbitMQ Testcontainers) |
| 16 | Integración | `fitstore-common/src/test` (Garage en Testcontainers) |
| 18 | CI | `.gitignore` + paso **gitleaks** en `ci.yml` (el *secret scanning* nativo de GitHub no está disponible en repos privados del plan gratuito) |
| 21 | Build | `jacoco:check` en `./mvnw verify` (probar con un módulo por debajo del umbral) |
| 22 | CI | pasos de contratos en `ci.yml` y `scripts/contrato.sh`; `ContratoOpenApiTest` en cada esqueleto |

**Núcleo a cubrir por completo (estrategia multiagente V0):** `fitstore-common` (outbox y su publicador, consumidor idempotente y DLQ, `FileStorage` y sus adaptadores, manejo de errores `ApiResponse`, *resource server*), filtros del gateway (JWT, rate limiting, bloqueo de `/internal`, fallback 503, `X-Request-Id`) y los repositorios con reglas (reserva atómica de stock, correlativo de reclamos, búsqueda jerárquica de tarifas).

**Convención de nombres:** cada prueba o verificación lleva el id del criterio (`@DisplayName("CA-0a-14 …")`; en `smoke.sh` y en los workflows, un comentario `# CA-0a-02`), para que `scripts/ca-cobertura.sh` lo encuentre.

**Pruebas de capa de datos por servicio (CA-0a-06):**

| Servicio | Repositorios |
|---|---|
| auth | `ClienteRepository`, `StaffRepository`, `DireccionRepository`, `TokenPasswordRepository` |
| catalog | `CategoriaRepository`, `ProductoRepository`, `VarianteRepository`, `StockRepository` (incluida la reserva atómica y la prueba de concurrencia), `MovimientoStockRepository`, `TramoDescuentoRepository` |
| settings | `ConfiguracionTiendaRepository`, `ImagenTiendaRepository`, `PoliticaRepository`, `ReclamoRepository` (+ correlativo concurrente), `UbigeoRepository` |
| order | `PedidoRepository`, `CarritoRepository` (Redis) |
| payment | `IntencionPagoRepository`, `TransaccionRepository`, `ReembolsoRepository`, `BoletaRepository` |
| delivery | `RepartidorRepository`, `TarifaEnvioRepository` (incluida la búsqueda jerárquica), `AgenciaRepository`, `EntregaRepository` |
| notification | `EnvioCorreoRepository`, `PedidoResumenRepository`, `MarcaTiendaRepository`, `PlantillaRepository` |

Total: **29 repositorios** con pruebas de datos.

## 8. Archivos y carpetas que toca

| Ruta | Acción | Después de cerrar 0a |
|---|---|---|
| `pom.xml`, `mvnw*`, `.mvn/`, `.gitignore`, `.editorconfig`, `README.md` | crea | `pom.xml` padre: congelado (cambios de versión en PR aparte) |
| `backend/fitstore-common/` | crea | **congelado** |
| `backend/config-server/`, `backend/discovery-server/` | crea | congelados |
| `backend/api-gateway/` | crea | **congelado** (las rutas ya cubren todas las specs) |
| `backend/<servicio>/` (7) | crea el esqueleto | pasa a su spec dueña (1–7) |
| `infra/config-repo/application.yml` | crea | **congelado** |
| `infra/config-repo/<servicio>*.yml` | crea | pasa a su spec dueña |
| `infra/scripts/ubigeo-a-sql` (settings, sección 6) | crea | pasa a la spec 3 |
| `.dockerignore` | crea | congelado |
| `infra/docker-compose.yml`, `docker-compose.dev.yml`, `.env.example`, `postgres/`, `garage/`, `smoke.sh`, `dev/` | crea | congelados (spec 8 agrega `docker-compose.cloud.yml` y `caddy/`) |
| `.gitleaks.toml` (allowlist de la clave de prueba, I2) y `.gitleaksignore` (huellas de falsos positivos) | crea | congelados |
| `scripts/contrato.sh`, `contracts/implementados.txt` | crea | `contrato.sh` congelado; `implementados.txt` lo amplía cada spec al cerrar |
| `.github/workflows/ci.yml`, `.github/workflows/stage.yml`, `.github/pull_request_template.md` | crea | congelados (0b agrega el job de frontend; la spec 8 crea `deploy.yml`) |
| `postman/fitstore-base.postman_collection.json`, `postman/local.postman_environment.json` | crea | cada spec agrega su colección |
| `docs/` | solo lectura | |

## 9. Definición de terminado

- [ ] CA-0a-01 a CA-0a-22 cumplidos (pruebas en CI y `smoke.sh` en Lyanna).
- [ ] Repositorio privado `cazabacheg/fitstore` con `dev` como rama por defecto y el flujo de la ADR-021 (sin reglas forzadas).
- [ ] `README.md` con cómo levantar el entorno dev (sin atribución de IA).
- [ ] Revisado y aprobado por el autor.

## Decisiones ✅

1. **Repositorio en GitHub:** privado, `cazabacheg/fitstore`, creado por el asistente (2026-10-18) con la documentación como primer commit.
2. **Adaptador Azure Blob:** fuera de 0a y diferido: la demo en Azure usa Garage en la VM (SPEC-8 I6).
