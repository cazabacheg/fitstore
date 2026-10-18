# 10. Comunicación síncrona con OpenFeign + Resilience4j

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** llamadas síncronas solo para lo que el usuario espera en pantalla (precio/stock/descuentos en carrito, **reserva de stock al crear pedido**, cotización de envío, datos del destinatario desde auth). Se implementan con **OpenFeign** (cliente HTTP declarativo: se define una interfaz Java con anotaciones y Spring genera la implementación; resuelve el destino por nombre vía Eureka y balancea entre instancias) y se protegen con **Resilience4j** (timeout, reintentos limitados y *circuit breaker*: si un servicio falla repetidamente, se deja de llamarlo por un tiempo y se responde de inmediato con un error controlado o un *fallback*, evitando que el fallo se propague en cascada).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
