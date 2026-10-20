# FitStore — Stack y versiones

> Versión: **v6 (aprobado)** — 2026-10-20. v6: versiones exactas fijadas en 0a.2 (`openhtmltopdf-pdfbox` 1.1.87, `greenmail-junit5` 2.1.14). v5: `spring-retry` 2.0.13 para el reintento del cliente de config (CA-0a-04) y etiquetas exactas de las imágenes fijadas en 0a.1. v4: JaCoCo, oasdiff y cobertura de Vitest (estrategia multiagente V0, SPEC-0a I7–I8). v3: Azure Blob diferido (SPEC-8 I6). v2: se adopta MapStruct. Versiones consultadas en Maven Central y npm el 2026-10-18.
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
| Reintento del config client | `org.springframework.retry:spring-retry` (el config client 5.0.x la usa internamente; ni Boot 4 ni Cloud 2025.1 la gestionan) | **2.0.13** |
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
| PDF (solo payment) | `io.github.openhtmltopdf:openhtmltopdf-pdfbox` | **1.1.87** (fijada en 0a.2; las 1.1.88+ tenían menos de dos semanas) |
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
| SMTP de prueba (notification) | `com.icegreen:greenmail-junit5` | **2.1.14** |
| Cobertura (backend) | `org.jacoco:jacoco-maven-plugin` | **0.8.15** |
| Cobertura (frontend) | `@vitest/coverage-v8` | la misma versión que Vitest (la fija 0b) |
| Contrato vs. código | `oasdiff` (imagen `tufin/oasdiff`) | **v1.33.0** |
| Carga | k6 (binario / imagen `grafana/k6`) | — |

**Testcontainers 2.0** renombró los módulos (`testcontainers-postgresql` en vez de `postgresql`) y cambió algunos paquetes; los ejemplos de 1.x necesitan ajustes.

## 4. Infraestructura (imágenes Docker)

| Componente | Imagen | Nota |
|---|---|---|
| PostgreSQL | `postgres:17.10` | Una instancia, 7 bases |
| Redis | `redis:8.10.1` | |
| RabbitMQ | `rabbitmq:4.3.5-management` | |
| Garage | `dxflrs/garage:v2.4.1` | Inicializado por `garage-init` con `curlimages/curl:8.22.0` (la imagen de Garage no trae shell) |
| Jaeger | `jaegertracing/jaeger:2.21.0` (all-in-one) | |
| Mailpit | `axllent/mailpit:v1.31.1` | Solo dev |
| Caddy | `caddy:2` | Solo cloud |
| Servicios Java | `eclipse-temurin:21.0.12.1_1-jdk` (compilación) y `-jre` (ejecución) | Imagen por servicio; Maven Wrapper 3.9.14 |
| Diagramas | `structurizr/structurizr`, `plantuml/plantuml` | Solo documentación |

Etiquetas fijadas en 0a.1 (2026-10-19), todas con más de dos semanas de publicadas; nunca `latest`. Caddy se fija en la spec 8.

## 5. Por verificar en la prueba de concepto

1. Spring Cloud Gateway (WebFlux) + `RequestRateLimiter` con Redis en Boot 4.0.8.
2. Eureka + OpenFeign + Resilience4j con Boot 4.0.8.
3. `spring-boot-starter-opentelemetry` exportando a Jaeger.
4. Testcontainers 2.0 con `@DataJpaTest` y `@ServiceConnection`.
5. Sandbox de Culqi (tarjeta y Yape) y Mercado Pago (R16).
