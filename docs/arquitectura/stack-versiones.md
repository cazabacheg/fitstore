# FitStore — Stack y versiones

> Versión: **v3 (aprobado)** — 2026-10-19. v3: Azure Blob diferido (SPEC-8 I6). v2: se adopta MapStruct. Versiones consultadas en Maven Central y npm el 2026-10-18.
> Todas las secciones 10.4 de los micros remiten a esta tabla; cada servicio solo lista **qué** usa, no la versión.
> Regla: las versiones se fijan al crear el repositorio y se actualizan solo con un motivo (seguridad o corrección).

## 1. Base

| Pieza | Versión | Nota |
|---|---|---|
| Java (JDK) | **21** (LTS) | Lyanna tiene OpenJDK 21.0.12 |
| Maven | 3.9.x (wrapper `mvnw`) | Lyanna tiene 3.8.7; el wrapper fija la versión del proyecto |
| **Spring Boot** | **4.0.8** | Última de la línea 4.0 (la del repo de clase usa 4.0.6) |
| **Spring Cloud** | **2025.1.3** ("Oakwood") | Compatible con Boot 4.0.x |
| Node.js | 22 LTS | Para Angular |
| **Angular** (CLI, core, Material) | **22.2.x** | Storefront y backoffice |
| Tailwind CSS | 4.3.x | Storefront |

**Por qué no Spring Boot 4.1.1** (ya publicado): necesita Spring Cloud 2026.0, que hoy solo está en *milestone* (2026.0.0-M1). Mezclar Boot 4.1 con Cloud 2025.1 no está soportado. Se queda en **4.0.8 + 2025.1.3** (ADR-001); migrar a 4.1 cuando salga Cloud 2026.0 GA sería un cambio de versión en el POM padre.

## 2. Librerías del backend

Gestionadas por los BOM de Spring Boot y Spring Cloud salvo que se indique versión.

| Uso | Artefacto | Versión |
|---|---|---|
| Web (MVC) | `spring-boot-starter-webmvc` | BOM Boot |
| Validación | `spring-boot-starter-validation` | BOM Boot |
| JPA | `spring-boot-starter-data-jpa` + driver `org.postgresql:postgresql` | BOM Boot (driver 42.7.x) |
| Migraciones | `spring-boot-starter-flyway` + `flyway-database-postgresql` | BOM Boot |
| Redis | `spring-boot-starter-data-redis` | BOM Boot |
| RabbitMQ | `spring-boot-starter-amqp` | BOM Boot |
| Seguridad | `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (validar JWT) | BOM Boot |
| Emisión de JWT (solo auth) | `spring-security-oauth2-jose` (Nimbus, incluido en Spring Security) | BOM Boot |
| Actuator | `spring-boot-starter-actuator` | BOM Boot |
| Trazas | `spring-boot-starter-opentelemetry` (Micrometer Tracing + OTLP) | BOM Boot |
| Correo (solo notification) | `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf` | BOM Boot |
| Config client | `spring-cloud-starter-config` | BOM Cloud |
| Eureka client | `spring-cloud-starter-netflix-eureka-client` | BOM Cloud |
| Feign | `spring-cloud-starter-openfeign` | BOM Cloud |
| Resilience4j | `spring-cloud-starter-circuitbreaker-resilience4j` | BOM Cloud (Resilience4j 2.x) |
| Gateway (solo gateway) | `spring-cloud-starter-gateway-server-webflux` + `spring-boot-starter-data-redis-reactive` | BOM Cloud |
| Config server / Eureka server | `spring-cloud-config-server` / `spring-cloud-starter-netflix-eureka-server` | BOM Cloud |
| OpenAPI | `org.springdoc:springdoc-openapi-starter-webmvc-ui` (gateway: `-webflux-ui`) | **3.1.1** |
| Lombok | `org.projectlombok:lombok` | **1.18.48** |
| Mapeo DTO ↔ entidad | `org.mapstruct:mapstruct` + `mapstruct-processor`, con `org.projectlombok:lombok-mapstruct-binding` | **1.6.3** (binding **0.2.0**) |
| S3 (Garage / AWS) | `software.amazon.awssdk:s3` (BOM `bom`) | **2.56.x** |
| Azure Blob (adaptador alternativo) | `com.azure:azure-storage-blob` (BOM `azure-sdk-bom`) | **diferido** (la demo usa Garage en la VM, SPEC-8 I6); se fija si se implementa |
| Imágenes (solo catalog, settings) | `net.coobird:thumbnailator` | **0.4.21** |
| PDF (solo payment) | `io.github.openhtmltopdf:openhtmltopdf-pdfbox` | **1.1.x** |
| Carga masiva (solo catalog) | `org.apache.poi:poi-ooxml`, `org.apache.commons:commons-csv` | **5.5.1**, **1.14.1** |
| Mercado Pago (solo payment) | `com.mercadopago:sdk-java` | **3.7.0** |
| Culqi (solo payment) | Cliente HTTP propio (`RestClient`) sobre su API REST | — |

**Mapeo DTO ↔ entidad:** DTOs como `record` de Java y **MapStruct** (`@Mapper(componentModel = "spring")`, un mapper por agregado). En el `maven-compiler-plugin` los procesadores van en este orden: `lombok`, `lombok-mapstruct-binding`, `mapstruct-processor`. Se activa `-Amapstruct.unmappedTargetPolicy=ERROR` para que un campo sin mapear falle al compilar.

**JSON:** Spring Boot 4 usa **Jackson 3** (paquete `tools.jackson`). Ojo al copiar ejemplos antiguos con `com.fasterxml.jackson`.

## 3. Pruebas

| Uso | Artefacto | Versión |
|---|---|---|
| Base | `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito) | BOM Boot |
| Capa de datos (AP1) | `spring-boot-starter-data-jpa-test` (`@DataJpaTest`) | BOM Boot |
| Contenedores | `org.testcontainers:testcontainers-postgresql`, `-rabbitmq`, `-junit-jupiter` (BOM `testcontainers-bom`) | **2.0.5** |
| Redis en pruebas | contenedor genérico `redis:8` con Testcontainers | — |
| Seguridad | `spring-security-test` | BOM Boot |
| HTTP simulado (pasarelas, Feign) | `org.wiremock:wiremock-standalone` | **3.13.x** (la 4.0 sigue en beta) |
| SMTP de prueba (notification) | `com.icegreen:greenmail-junit5` | **2.1.x** |
| Carga | k6 (binario / imagen `grafana/k6`) | — |

**Testcontainers 2.0** renombró los módulos (`testcontainers-postgresql` en vez de `postgresql`) y cambió algunos paquetes; los ejemplos de 1.x necesitan ajustes.

## 4. Infraestructura (imágenes Docker)

| Componente | Imagen | Nota |
|---|---|---|
| PostgreSQL | `postgres:17` | Una instancia, 7 bases |
| Redis | `redis:8` | |
| RabbitMQ | `rabbitmq:4-management` | |
| Garage | `dxflrs/garage` | Etiqueta exacta al crear el compose |
| Jaeger | `jaegertracing/jaeger` (v2, all-in-one) | |
| Mailpit | `axllent/mailpit` | Solo dev |
| Caddy | `caddy:2` | Solo cloud |
| Servicios Java | `eclipse-temurin:21-jre` (base) | Imagen por servicio |
| Diagramas | `structurizr/structurizr`, `plantuml/plantuml` | Solo documentación |

Las etiquetas exactas (`postgres:17.x`, etc.) se fijan en `infra/docker-compose.yml` al crearlo; nunca `latest`.

## 5. Por verificar en la prueba de concepto

1. Spring Cloud Gateway (WebFlux) + `RequestRateLimiter` con Redis en Boot 4.0.8.
2. Eureka + OpenFeign + Resilience4j con Boot 4.0.8.
3. `spring-boot-starter-opentelemetry` exportando a Jaeger.
4. Testcontainers 2.0 con `@DataJpaTest` y `@ServiceConnection`.
5. Sandbox de Culqi (tarjeta y Yape) y Mercado Pago (R16).
