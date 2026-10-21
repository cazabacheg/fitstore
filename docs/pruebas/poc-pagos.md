# Prueba de concepto de pagos (SPEC-5, P1)

> Guion para el sandbox real de Mercado Pago y para la demostración con el proveedor simulado. Alcance de esta entrega: **Mercado Pago (sandbox)** y el **proveedor simulado**; Culqi, Yape y los reembolsos automáticos son extensión de alcance ([`extension-alcance.md`](../implementacion/extension-alcance.md)).
>
> Estado: **pendiente de credenciales de prueba de Mercado Pago**. El backend, el Payment Brick y los webhooks se probaron con WireMock (sin internet); este guion se corre cuando lleguen las credenciales y se registran aquí los resultados.

## 1. Preparación

1. En el panel de desarrolladores de Mercado Pago crea una aplicación de prueba y copia sus credenciales de **prueba** (empiezan con `TEST-`).
2. En el `.env` (nunca en el repositorio) completa:

   | Variable | Qué es |
   |---|---|
   | `MP_PUBLIC_KEY` | Clave pública: la usa el navegador para el Payment Brick |
   | `MERCADOPAGO_ACCESS_TOKEN` | Token de acceso: lo usa el backend para cobrar y consultar |
   | `MP_WEBHOOK_SECRET` | Secreto de las notificaciones (panel de la aplicación, sección Webhooks) |
   | `PAGOS_SIMULADO_HABILITADO` | `true` en dev y en la demo; `false` por defecto |

3. En el backoffice de la tienda (Configuración, medios de pago) deja habilitado **Mercado Pago**.
4. Levanta la plataforma (`docker compose`) y entra a la tienda con la cuenta de prueba.

## 2. Cobro con tarjeta de prueba (Payment Brick)

1. Crea un pedido y entra a `/pago/FS-xxxxxx`. Elige **Mercado Pago**: aparece el Brick embebido.
2. Paga con una tarjeta de prueba de Mercado Pago (Perú) con titular **APRO** para aprobar y **OTHE** para el rechazo general; **FUND** para fondos insuficientes (apúntalas abajo si cambian).
3. Resultado esperado: `¡Pago aprobado!`, transacción `APROBADA`, intención `PAGADA`, evento `PagoConfirmado` y boleta `B001-…` descargable en unos segundos.
4. Repite con titular **FUND**: `402` con "Fondos insuficientes"; la página ofrece "Intentar de nuevo" (otra Idempotency-Key).

## 3. Webhooks

- En local las pasarelas no llegan al servidor y no se usa un túnel (P5): el **verificador** (cada 2 minutos) consulta los pagos `PROCESANDO` y los resuelve. Para verlo, deja un pago en curso (tarjeta `4000 0000 0000 0119` del simulado) y espera.
- Para probar el webhook real hay que exponer `/api/v1/pagos/webhooks/mercadopago` (nube, spec 8). La firma `x-signature` se valida con `MP_WEBHOOK_SECRET`; la colección Postman trae un paso con firma inválida (401) y otro que firma el aviso con el secreto.

## 4. Anotar al ejecutar

| Pregunta | Resultado |
|---|---|
| Tarjetas y titulares de prueba que funcionan | _por completar_ |
| ¿El sandbox exige 3-D Secure? (si sí, se agrega a la spec) | _por completar_ |
| ¿Llegan los webhooks al servidor expuesto? ¿Con qué cabeceras? | _por completar_ |
| ¿`X-Idempotency-Key` evita el cargo doble en el sandbox? | _por completar_ |

## 5. Demostración sin credenciales: proveedor simulado

Con `PAGOS_SIMULADO_HABILITADO=true` aparece **Pago de prueba** (con el aviso de que no es un cobro real). Escribe el número de tarjeta de prueba:

| Tarjeta | Resultado |
|---|---|
| `4111 1111 1111 1111` | Se aprueba |
| `4000 0000 0000 0002` | Se rechaza por fondos insuficientes |
| `4000 0000 0000 0119` | Queda `PROCESANDO` (202) y el verificador la aprueba en la siguiente pasada |
| Cualquier otro número válido | Se aprueba |

Flujo de la demo: pagar → "¡Pago aprobado!" → descargar la boleta → en el backoffice, **Pagos** muestra el pago, sus intentos y la boleta.

## 6. Colección Postman

`postman/payment.postman_collection.json`: página de pago, pago simulado aprobado, rechazado y en curso, idempotencia, doble cobro, descarga de boleta (propia y ajena), webhook con firma inválida y válida, 429 por exceso de intentos y las consultas del backoffice (con el 403 al OPERADOR). Sin pasos de reembolso (extensión de alcance).
