# 20. Redis para rate limiting, carrito y caché

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Contexto:** hay puntos de entrada críticos (login, carrito, lecturas que hacen todos los visitantes) donde conviene responder rápido y protegerse de abuso.
- **Decisión:** una instancia de **Redis** compartida, con prefijos de clave por servicio:
  - **Gateway:** rate limiting de login y endpoints sensibles (`RequestRateLimiter` de Spring Cloud Gateway, por IP y por usuario).
  - **auth-service:** contador de intentos fallidos para bloqueo temporal (RF-IAM-08) y registro de refresh tokens revocados.
  - **order-service:** carrito (ADR-012).
  - **Caché de lecturas críticas** (Spring Cache): configuración pública de la tienda (`settings`), árbol de categorías, listados y detalle de productos (`catalog`), con invalidación por evento o TTL corto.
- **Alternativas:** rate limiting en memoria con Bucket4j (sin contenedor extra, pero no se comparte entre servicios ni sobrevive reinicios); sin caché.
- **Consecuencias:** +~64 MB de RAM y un contenedor; una tecnología más en la exposición (útil para mostrar rendimiento con k6 antes/después de la caché). Redis no es fuente de verdad salvo para el carrito; si se cae, el resto del sistema sigue funcionando (degradación sin caché).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
