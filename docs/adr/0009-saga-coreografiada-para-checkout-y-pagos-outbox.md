# 9. Saga coreografiada para checkout y pagos + outbox

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** arquitectura orientada a eventos con **coreografía** (sin orquestador central): cada servicio reacciona a eventos y publica los suyos. El checkout y el pago forman una **saga**: una secuencia de transacciones locales en distintos servicios, donde cada paso tiene su **compensación** si algo falla más adelante. La reserva de stock es síncrona (respuesta inmediata al cliente, ADR-010); el resto, por eventos.
- **Publicación confiable:** **patrón Outbox** (el evento se guarda en una tabla `outbox` en la misma transacción que el cambio de estado y un publicador lo envía a RabbitMQ) en order, catalog y payment. Consumidores **idempotentes** (tabla de eventos procesados).
- **Estado de la saga:** lo lleva la máquina de estados del pedido (`order-service`) y la intención de pago (`payment-service`); no hay tabla de saga aparte.

**Pasos y compensaciones de la saga de checkout/pago:**

| # | Servicio | Transacción local | Si falla después → compensación |
|---|---|---|---|
| T1 | order + catalog | Crear pedido `PENDIENTE_PAGO` y reservar stock (síncrono). Si no hay stock, no se crea el pedido | Liberar reserva (`PedidoCancelado` / `PedidoExpirado`) |
| T2 | payment | Al recibir `PedidoCreado`, crear intención de pago `PENDIENTE` con monto e ítems | Anular intención |
| T3 | payment | Cargo con el token (**paso pivote**: si se cobra, ya no se "deshace", solo se reembolsa) → `PagoConfirmado` / `PagoRechazado` | Reembolso |
| T4 | order | Pedido → `PAGADO`, publica `PedidoPagado` | — |
| T5 | catalog | Confirmar reserva (descontar stock definitivo) | Reingresar stock (si luego se cancela) |
| T6 | payment | Emitir boleta simulada → `BoletaEmitida` | — |

**Casos de falla contemplados:**

- **Pago rechazado:** la intención sigue abierta; el cliente puede reintentar (con otra tarjeta o proveedor) hasta que el pedido expire. Al expirar: `PedidoExpirado` → catalog libera la reserva, payment anula la intención.
- **Pago aprobado después de que el pedido expiró** (carrera entre webhook y expiración): order rechaza la transición y publica `PagoNoAplicable` → payment **reembolsa automáticamente**.
- **Cancelación de un pedido pagado** (antes del despacho): `PedidoCancelado` → payment reembolsa, catalog reingresa el stock, notification avisa.
- **Reembolso fallido:** payment reintenta con espera creciente; si persiste, marca `REEMBOLSO_FALLIDO` y alerta al admin (intervención manual en backoffice).
- **Mensajes duplicados** (webhook + respuesta síncrona del proveedor, reintentos de RabbitMQ): idempotencia por id de intención y por id de evento.
- **Tiempos:** la reserva de stock expira **después** que el pedido (p. ej. 35 vs 30 min), para que el stock no se libere mientras hay un pago en curso.

- **Alternativa descartada:** saga orquestada (un orquestador central que dirige cada paso). Más explícita, pero con la reserva síncrona y pocos pasos la coreografía queda simple y desacoplada.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
