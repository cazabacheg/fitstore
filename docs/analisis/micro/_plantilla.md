# FitStore — Análisis micro: `<servicio>`

> Versión: **v1 (borrador)** — AAAA-MM-DD. Fuente: [`01-analisis-macro.md`](../01-analisis-macro.md).
> Estados: ✅ decidido · 🟡 propuesto · ❓ abierto · 🕓 diferido

**Historial:**
- v1 (AAAA-MM-DD): borrador inicial.

**Estado de revisión por sección:** 1 ⏳ · 2 ⏳ · 3 ⏳ · 4 ⏳ · 5 ⏳ · 6 ⏳ · 7 ⏳ · 8 ⏳ · 9 ⏳ · 10 🕓

---

## 1. Responsabilidad y límites

Qué hace el servicio, qué no hace, RF y CU que cubre (con referencia al macro).

## 2. Modelo de dominio

Entidades, atributos, relaciones e invariantes. Diagrama de clases (mermaid).

## 3. Modelo de datos

Tablas, claves, índices, restricciones; migración inicial de Flyway. Estructuras en Redis si aplica.

## 4. Casos de uso internos

Flujo paso a paso por RF: validaciones, reglas, errores y códigos HTTP.

## 5. Estados

Máquina de estados (mermaid `stateDiagram`) cuando aplique.

## 6. Eventos

Publicados y consumidos: nombre, campos, exchange/cola, idempotencia, outbox.

## 7. Dependencias síncronas

Llamadas Feign salientes y entrantes; timeout, reintentos, circuit breaker y fallback.

## 8. Seguridad

Operación → rol requerido; datos sensibles; validaciones.

## 9. Pruebas

Capa de datos (`@DataJpaTest` / Testcontainers), servicio (unitarias), API (MockMvc / Postman).

## 10. Diseño técnico 🕓

> **Diferido:** se completa después de terminar las secciones 1–9 de todos los servicios y de elaborar los diagramas C1 (contexto) y C2 (contenedores) del sistema.

- 10.1 Diagrama de componentes (C4 nivel 3)
- 10.2 Diagrama de código (C4 nivel 4) — solo para las partes críticas
- 10.3 Estructura de paquetes y capas
- 10.4 Dependencias y librerías (starters de Spring, SDKs) con versión
- 10.5 Configuración (propiedades, perfiles, variables de entorno, puerto)
- 10.6 Componentes de infraestructura que usa (PostgreSQL, Redis, RabbitMQ, Garage, etc.)
