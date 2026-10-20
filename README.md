# FitStore

Plantilla white-label de e-commerce de ropa y moda para MYPEs (primer caso: ropa deportiva de Gamarra). Microservicios Spring Boot + Spring Cloud, dos frontends Angular y despliegue con contenedores.

Proyecto final de Desarrollo de Aplicaciones Web II y Desarrollo de Servicios Web II — Cibertec.

**Estado:** análisis, arquitectura y especificaciones completos; plataforma backend (SPEC-0a) construida; implementación de las specs de negocio en curso.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/analisis/01-analisis-macro.md`](docs/analisis/01-analisis-macro.md) | Visión, casos de uso, requisitos, bounded contexts, decisiones, riesgos |
| [`docs/analisis/micro/`](docs/analisis/micro/) | Análisis por servicio (dominio, datos, casos de uso, eventos, diseño técnico) |
| [`docs/arquitectura/c4-sistema.md`](docs/arquitectura/c4-sistema.md) | Diagramas C4 (contexto, contenedores, componentes, despliegue) |
| [`docs/arquitectura/stack-versiones.md`](docs/arquitectura/stack-versiones.md) | Stack y versiones |
| [`docs/adr/`](docs/adr/) | Decisiones de arquitectura |
| [`docs/specs/`](docs/specs/) | Especificaciones por servicio |

## Entorno de desarrollo

Requisitos: JDK 21 y Docker con Compose. El Maven Wrapper (`./mvnw`) fija la versión de Maven del proyecto.

### Compilar y probar

```bash
./mvnw verify                                   # compila los 11 módulos y corre todas las pruebas (necesita Docker)
./mvnw -pl backend/api-gateway -am verify       # solo un módulo y lo que necesita
```

El build exige al menos el 80 % de líneas cubiertas por módulo (JaCoCo; se excluyen configuración, DTO y clases
`Application`).

### Levantar la infraestructura

```bash
cp infra/.env.example infra/.env               # valores solo de desarrollo local; .env no se sube a Git
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d
```

Para levantar solo la infraestructura y el punto de entrada:

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --wait \
  postgres redis rabbitmq garage garage-init jaeger mailpit config-server discovery-server
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --wait --no-deps api-gateway
```

| Consola | URL |
|---|---|
| API Gateway | http://localhost:8080 (Swagger: `/swagger-ui`) |
| Eureka | http://localhost:8761 |
| RabbitMQ | http://localhost:15672 |
| Jaeger | http://localhost:16686 |
| Mailpit | http://localhost:8025 |

Mientras auth no firme los tokens (spec 1), el gateway y los servicios validan con el JWKS de prueba de `infra/dev/`
(solo desarrollo).

### Levantar la plataforma completa y correr el humo

Con un solo comando se construyen las imágenes, se levanta toda la plataforma (infraestructura, config-server, Eureka,
los 7 servicios de negocio y el gateway) y se verifica que quedó sana:

```bash
infra/smoke.sh        # crea infra/.env si falta, construye, levanta, espera healthy y verifica; código 0 solo si todo pasa
```

Necesita Docker con Compose, `curl` y `jq`, y unos 4,5 GB de memoria libres para los contenedores. El humo verifica:
todos los contenedores *healthy* en menos de 5 minutos (CA-0a-02), `api-gateway` y los 7 servicios en UP en Eureka
(CA-0a-03), las migraciones de Flyway aplicadas en cada base con Hibernate en `validate` (CA-0a-05), que el usuario de
un servicio no puede entrar a la base de otro (CA-0a-07), que `/swagger-ui` del gateway ofrece los 7 servicios
(CA-0a-17) y que cada contenedor tiene `mem_limit` con una suma de 6,5 GB como máximo (CA-0a-19).

Variables opcionales: `SMOKE_TIMEOUT` (segundos para el arranque, 300 por defecto), `SMOKE_SIN_ARRANQUE=1` (verifica una
plataforma que ya está corriendo) y `SMOKE_BAJAR=1` (baja el compose y borra los volúmenes al terminar).

Colección Postman base (salud del gateway y documentación de los 7 servicios a través del gateway), con la plataforma
arriba:

```bash
npx newman@6.2.2 run postman/fitstore-base.postman_collection.json --environment postman/local.postman_environment.json
```

### Contratos y CI

```bash
scripts/contrato.sh auth-service   # compara contracts/openapi/<servicio>.yaml con el OpenAPI que genera el servicio (oasdiff)
```

`contracts/implementados.txt` lista los servicios cuyo contrato ya implementa el código; el CI solo compara esos.
GitHub Actions: `ci.yml` (PR y push a `dev`: compilación y pruebas por módulo con caché de Maven, gitleaks y validación
de contratos) y `stage.yml` (PR y push a `stage`, PR a `main`: imágenes, compose completo, `smoke.sh` y Newman).
Los PR usan la plantilla de `.github/pull_request_template.md`.
