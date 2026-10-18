# 19. Reducción de granularidad: de 12 a 10 servicios

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Contexto:** la primera versión (v1) asignaba un microservicio a cada bounded context: 3 de infraestructura + 9 de negocio (auth, catalog, inventory, cart, order, payment, delivery, notification, settings). Se revisó el balance costo/beneficio: cada servicio adicional suma ~300 MB de RAM, un contenedor, configuración, contratos y puntos de fallo, en una VM de 8 GB y con un solo desarrollador.
- **Criterio:** agrupar contextos que **casi siempre cambian juntos** o que **necesitan consistencia inmediata** entre sí; mantener separados los que tienen actores, ciclo de vida o fronteras externas propios.
- **Decisión:**
  - `catalog` + `inventory` → **`catalog-service`**: una variante (SKU) no existe sin su stock y el catálogo muestra disponibilidad en todo momento; separados obligaban a sincronizar por eventos sin beneficio real.
  - `cart` + `order` → **`order-service`**: el carrito es un pedido en borrador (mismo cliente, ítems y validaciones); juntos, el checkout es una operación local. (En v2 esto también eliminaba Redis; en v5 se reincorporó por otras razones, ver ADR-020.)
  - Se **mantienen separados**: `payment` (frontera con pasarelas externas, aislamiento PCI, patrón Strategy), `delivery` (actores, estados y módulo de backoffice propios; diferenciador "estilo Shopify"), `notification` y `settings` (sin cohesión con otros), `auth` (identidad siempre aparte).
- **Resultado:** 10 servicios (3 de infraestructura + 7 de negocio).
- **Alternativa descartada:** ir a 9 integrando `payment` en `order` (pierde el aislamiento de pasarelas y la saga entre servicios).
- **Consecuencias:** ~600 MB menos de RAM (dos procesos Spring menos); menos contratos; saga más simple (order ↔ catalog ↔ payment ↔ delivery). Inventario y carrito siguen siendo módulos con fronteras claras dentro de su servicio, por lo que podrían extraerse en el futuro si el volumen lo justifica.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
