# FitStore

Plantilla white-label de e-commerce de ropa y moda para MYPEs (primer caso: ropa deportiva de Gamarra). Microservicios Spring Boot + Spring Cloud, dos frontends Angular y despliegue con contenedores.

Proyecto final de Desarrollo de Aplicaciones Web II y Desarrollo de Servicios Web II — Cibertec.

**Estado:** análisis y arquitectura completos; en etapa de especificaciones.

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

Los siete servicios de negocio se completan en los pasos 0a.2 y 0a.3 de la SPEC-0a. Mientras auth no firme los
tokens (spec 1), el gateway y los servicios validan con el JWKS de prueba de `infra/dev/` (solo desarrollo).
