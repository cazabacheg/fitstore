# 12. Carrito en Redis dentro de `order-service`, con carrito anónimo

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Contexto:** v1 proponía `cart-service` con Redis; v2 (al agrupar el carrito en `order-service`, ADR-019) lo movía a PostgreSQL y eliminaba Redis. En la revisión de RNF se decidió **reincorporar Redis** por su valor en puntos críticos (ADR-020).
- **Decisión:** el carrito vive en **Redis** (estructura hash por carrito, con TTL) dentro del módulo carrito de `order-service`. Carrito anónimo con id de sesión en el navegador y fusión al iniciar sesión. Al confirmar el checkout, el carrito se convierte en pedido persistido en PostgreSQL (`order_db`).
- **Alternativas:** PostgreSQL (más simple, pero expiración manual y más carga en lecturas frecuentes), carrito solo en el navegador (no persiste entre dispositivos).
- **Consecuencias:** expiración nativa por TTL, lecturas/escrituras muy rápidas. Las pruebas de capa de datos del carrito usan Spring Data Redis con Testcontainers (el resto, `@DataJpaTest`).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
