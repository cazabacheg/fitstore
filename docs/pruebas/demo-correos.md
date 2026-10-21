# Guion de la demo de correos (CA-7-29)

> SPEC-7 · Se corre con la plataforma levantada en desarrollo y Mailpit abierto en <http://localhost:8025>.
> Es la verificación manual de **CA-7-29**: una compra completa (registro, pago, entrega) y una devolución, con cada
> correo en Mailpit con la marca de la tienda, en HTML y texto plano, y la boleta adjunta.

## Antes de empezar

1. Levanta la plataforma: `docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d` y espera a que todo esté `healthy`.
2. Abre **Mailpit** (<http://localhost:8025>) y vacía el buzón ("Delete all") para ver solo los correos de la demo.
3. Ten abiertos la tienda (<http://localhost:4200>) y el backoffice (<http://localhost:4300>), con el ADMIN inicial (`FITSTORE_ADMIN_EMAIL` y `FITSTORE_ADMIN_PASSWORD`) y la cuenta del OPERADOR de demostración (`diana.flores@fitstore.demo`).
4. En el compose de desarrollo el despachador envía hasta 10 correos por segundo (SPEC-7 N6), así que cada correo aparece en Mailpit a los pocos segundos.

## Paso a paso

| # | Qué haces | Correo que debe aparecer en Mailpit | Qué mirar |
|---|---|---|---|
| 1 | En la tienda, **regístrate** como clienta nueva (por ejemplo `ana.demo@fitstore.demo`) | **Te damos la bienvenida a FitStore, Ana** (C1) | Logo y colores de la marca en la cabecera; pie con dirección, horario, redes y políticas |
| 2 | Agrega productos al carrito, haz el **checkout** (delivery o agencia) y **paga** con el pago de prueba | **¡Gracias por tu compra, Ana! Pedido FS-…** (C5) | Resumen de los productos con montos `S/ 0.00`, la modalidad de entrega y la **boleta `B001-….pdf` adjunta** |
| 3 | En el backoffice, con el OPERADOR: pasa el pedido a **en preparación** y **listo para despacho** | (ninguno: antes del despacho no se envía nada) | Que no llegue ningún correo en este paso |
| 4 | En entregas, asigna un repartidor y márcala **en ruta** (o, por agencia, **enviada** con su código de seguimiento) | **Tu pedido FS-… va en camino** (C8) | En delivery local: fecha y repartidor (nunca su teléfono). En agencia: agencia, código de seguimiento y enlace de rastreo |
| 5 | Marca la entrega como **entregada** | **¡Tu pedido FS-… fue entregado!** (C11) | Confirmación y agradecimiento |
| 6 | **Devolución**: en una segunda compra deja que la entrega **no se concrete** y márcala **devuelta a la tienda** | **No pudimos entregar tu pedido** (C9) y luego **Tu pedido FS-… regresó a la tienda** (C14) | El motivo y los datos de contacto de la tienda |
| 7 | Cancela un pedido ya pagado desde la tienda | **Tu pedido FS-… fue cancelado** (C6) | Mensaje de que se procesará el reembolso |
| 8 | Registra un reclamo en el Libro de Reclamaciones | **Copia de tu reclamo RC-…** (C12) a la clienta y **Nuevo reclamo RC-…** (T4) al correo de alertas | La hoja completa y la fecha límite en formato es-PE (`jueves 5 de noviembre`) |

En cada correo comprueba, en Mailpit:

- la pestaña **HTML** (con la marca: logo, color primario en cabecera y botones, texto legible sobre el color) y la pestaña **Text** (versión en texto plano);
- que **no aparezca el DNI** en el cuerpo (solo va dentro de la boleta);
- que las fechas y montos estén en formato es-PE.

## Registro de envíos y reenvío

1. En el backoffice entra a **Notificaciones** (solo ADMIN): ves los correos de la compra del más reciente al más antiguo, junto con los de los pedidos de demostración `FS-000101` a `FS-000111`.
2. Filtra por estado **Fallido**: aparece el reembolso de `FS-000110`. Haz clic en su asunto para abrir el **panel de detalle** (cada intento con su fecha y el último error).
3. Pulsa **Reenviar**, confirma y comprueba que vuelve a **Pendiente** y que el correo llega a Mailpit con la marca y los textos actuales.
4. Entra con el OPERADOR: no ve el menú Notificaciones y, por URL, recibe la página 403.

## Correo fallido (SMTP caído)

1. Detén Mailpit (`docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml stop mailpit`) y haz una compra: el pedido se confirma igual y el correo queda **Pendiente** con reintentos a los 1, 5, 15 y 60 minutos.
2. Vuelve a levantar Mailpit (`start mailpit`): el siguiente reintento lo envía. Si ya quedó **Fallido**, reenvíalo desde el registro.

## Verificación automática con Postman

Después de la compra, corre la carpeta **CA-7-29** de `postman/notification.postman_collection.json` con `verificarMailpit=true`
(`mailpitUrl` apunta a Mailpit): comprueba que hay correos, que la confirmación trae HTML, texto plano y la boleta PDF adjunta, y que existe el correo de "va en camino".

```bash
npx newman run postman/notification.postman_collection.json --environment postman/local.postman_environment.json \
  --env-var adminEmail=$FITSTORE_ADMIN_EMAIL --env-var adminPassword=$FITSTORE_ADMIN_PASSWORD \
  --env-var verificarMailpit=true
```

## Lo que no se muestra (extensión de alcance)

El editor de textos de los correos, la vista previa, "Enviar prueba a mi correo" y la limpieza de retención (HU-7-06 y HU-7-07) quedan fuera de esta entrega: los correos usan los textos predeterminados de las plantillas.
