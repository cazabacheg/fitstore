# Guion de la demo de FitStore

> SPEC-8 · CA-8-29 y CA-8-30 (más los pasos manuales de CA-8-23 y CA-8-25). Duración total: **20 minutos** (el tiempo de la exposición); para el video, el mismo recorrido sin los márgenes.
> Dominio: `https://tienda.fitstore.duckdns.org` (tienda) y `https://admin.fitstore.duckdns.org` (backoffice). Datos y cuentas de demo: [`ids-demo.md`](ids-demo.md). Despliegue y operación: [`../despliegue.md`](../despliegue.md).

## 0. Lista de verificación previa (hacerla 30 minutos antes)

- [ ] **VM encendida** y estable: `az vm start -g fitstore-rg -n fitstore-vm`; esperar unos 130 s. Comprobar `ssh fitstore@57.156.71.26 "cd fitstore && docker compose -f infra/docker-compose.yml -f infra/docker-compose.cloud.yml ps"` con todo *healthy*.
- [ ] **Certificados válidos:** abrir ambos dominios con el candado de HTTPS (Caddy los renueva solo; no borrar el volumen `caddy-datos`).
- [ ] **Datos de demo cargados:** el dashboard del backoffice muestra pedidos en varios estados y las entregas de FS-000101 a FS-000104.
- [ ] **Cuentas:** conocer `DEMO_PASSWORD` (cuentas demo: `lucia.mendoza@fitstore.demo` y el operador `diana.flores@fitstore.demo`) y las credenciales del ADMIN (`FITSTORE_ADMIN_EMAIL`).
- [ ] **Correos:** túnel abierto a Mailpit (`ssh -N -L 8025:localhost:8025 fitstore@57.156.71.26`) y la bandeja en `http://localhost:8025`. (Los correos de la demo salen a Mailpit, sin SES.)
- [ ] **Pasarela:** el proveedor simulado habilitado (`PAGOS_SIMULADO_HABILITADO=true`). Si se usa Mercado Pago sandbox: credenciales en el `.env` de la VM y el saldo de la **tarjeta de prueba** vigente.
- [ ] **Marca:** la tienda está con la marca FitStore (nombre y color arena `#C9B28C`) para poder cambiarla en vivo.
- [ ] Tener abiertas, en pestañas: la tienda, el backoffice (ya con sesión de ADMIN), Mailpit, Swagger, Jaeger (túnel `-L 16686:localhost:16686`), Postman y GitHub Actions.
- [ ] **Al terminar:** `az vm deallocate -g fitstore-rg -n fitstore-vm`.

## 1. Recorrido (con tiempos)

| Min | Qué se muestra | Qué se dice / qué se pulsa |
|---|---|---|
| 0:00 a 1:30 | **Landing y catálogo con filtros** | Landing de la tienda (hero, categorías, destacados). Entrar a *Productos*: filtrar por categoría, talla y color, buscar "legging". Mostrar que carga rápido en el celular (vista móvil del navegador) |
| 1:30 a 4:00 | **Compra con descuento por volumen** | Abrir *Legging Pro Compresión*, elegir color y talla, poner **3 unidades**: aparece el tramo "−5 %". Agregar al carrito, registrarse como cliente nuevo, ir al checkout, elegir dirección y *Delivery a domicilio*, ver el resumen (subtotal, descuento, envío) y confirmar |
| 4:00 a 6:00 | **Pago** | En *Paga tu pedido*: **Mercado Pago** (sandbox) si hay credenciales, o el **proveedor simulado** con la tarjeta `4111 1111 1111 1111` (aprueba). Mostrar "¡Pago aprobado!". Se menciona Culqi y Yape como medios adicionales previstos (extensión) |
| 6:00 a 7:30 | **Correos** | En Mailpit: la bienvenida y "¡Pedido confirmado!" con la **boleta PDF** adjunta |
| 7:30 a 10:30 | **Backoffice: dashboard, despacho y entrega** | Entrar al backoffice como OPERADOR: *Dashboard* (pedidos por estado, stock bajo, entregas de hoy, reclamos por vencer). Abrir el pedido nuevo: *Empezar a preparar*, *Marcar listo para despacho*. En *Entregas*: asignar repartidor, *En ruta*, *Entregada*. Volver a la tienda: el cliente ve **Entregado** en su pedido y los correos "en camino" y "entregado" en Mailpit |
| 10:30 a 11:30 | **Reembolso postventa** | *Extensión de alcance*: la devolución del dinero de un pedido entregado queda fuera de esta entrega. Se muestra FS-000109 (pedido entregado con reembolso parcial de los datos de demo) y se explica el flujo previsto |
| 11:30 a 13:00 | **Libro de Reclamaciones** | En la tienda: *Libro de Reclamaciones*, llenar la hoja, recibir el número `RC-…` y la copia por correo. En el backoffice (ADMIN): *Reclamos*, abrirlo y **responder**; el consumidor recibe la respuesta por correo |
| 13:00 a 15:30 | **Cambio de marca en vivo (dos marcas)** | Backoffice (ADMIN) > *Configuración de tienda*: pestaña *Identidad* (nombre **Kallpa**) y pestaña *Tema* (primario `#B3245C`, secundario `#22182E`, acento `#A3E635`); *Guardar*. Recargar la tienda: nombre y colores nuevos. Volver a FitStore con *Restaurar tema por defecto* y el nombre original |
| 15:30 a 20:00 | **Parte técnica** | (1) **Swagger con contraseña**: `https://admin.fitstore.duckdns.org/swagger-ui` pide usuario y contraseña; el selector lista los 7 servicios. (2) **Postman contra la nube**: las colecciones con el entorno `postman/entornos/cloud.json` (también corren en `deploy.yml` con Newman). (3) **Jaeger** (túnel): seguir la compra por su `X-Request-Id` a través de gateway, order, catalog, payment y notification. (4) **GitHub Actions**: `ci` (PR a `dev`), `stage` (compose completo + Newman + E2E con Playwright + Lighthouse) y `deploy` (al integrar en `main`; SSH a la VM, construcción, *healthy* y verificación). (5) Arquitectura: una VM, `docker-compose`, Caddy con HTTPS, memoria acotada (≤ 6,5 GB), respaldo diario |

## 2. Pasos manuales que cubren criterios de la nube

- **CA-8-23 Webhooks reales (solo si hay credenciales de Mercado Pago o Culqi):** registrar `https://tienda.fitstore.duckdns.org/api/v1/pagos/webhooks/mercadopago` (y `/culqi`) en el panel sandbox; pagar un pedido y comprobar en los registros de `payment-service` que el pago se confirmó **por webhook** (no solo por el verificador): `docker compose ... logs payment-service | grep -i webhook`.
- **CA-8-25 Proveedor simulado:** en *Paga tu pedido* aparece "Pago de prueba" aunque las pasarelas reales no estén configuradas; `desplegar.sh` verifica que `PAGOS_SIMULADO_HABILITADO=true`.
- **CA-8-24 (SES):** fuera de esta entrega por el desvío acordado: los correos salen a Mailpit.

## 3. Plan B por cada dependencia externa

| Si falla… | Hacer |
|---|---|
| Sandbox de Mercado Pago o Culqi caído, o sin saldo en la tarjeta de prueba | Pagar con el **proveedor simulado** (tarjeta `4111 1111 1111 1111`). Es el mismo flujo de punta a punta |
| Correo (hoy Mailpit en la VM) | Si se llegara a usar SES y cayera: `SMTP_HOST=mailpit` y `docker compose ... up -d notification-service`; los correos se ven por el túnel `-L 8025:localhost:8025` |
| DuckDNS o el certificado no responde | Mostrar la demo por IP no sirve con HTTPS; usar el túnel SSH al gateway (`ssh -L 8080:localhost:8080`) o la demo local en Lyanna (`docker compose` de dev) |
| La VM no arranca o tarda | Plan local: `infra/docker-compose.yml` + `docker-compose.dev.yml` en Lyanna con los mismos datos de demo (tienda en `localhost:4200`) |
| Internet de la sala | Video grabado del recorrido completo (mismo guion) |
| Un servicio no responde | El dashboard marca solo esa tarjeta como "No disponible" (CA-8-04): mostrarlo como resiliencia y usar *Reintentar* |

## 4. Rendimiento (CA-8-13)

Las pruebas de carga k6 de catálogo (p95 < 500 ms con 50 usuarios) y checkout (p95 < 2 s) las mide el lead en Lyanna antes de cada promoción a `main` y deja el resultado en el PR. En la demo se puede comentar el resultado.

## 5. Ensayo (CA-8-29 y CA-8-30)

Ensayar con cronómetro dos veces antes de la fecha; ajustar los minutos de la tabla si un bloque se pasa. Dejar el video grabado con el mismo orden.
