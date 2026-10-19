Precio en soles con cifras tabulares, precio original tachado y descuento por volumen.

**Úsalo** en tarjetas de producto, detalle, carrito, checkout, pago y pedidos.

**Lo que da el consumidor:** el monto (número del contrato, 2 decimales), opcionalmente el monto antes del descuento y el porcentaje. El formato es siempre `S/ 59.90` (pipe `precio` de la SPEC-0b, es-PE, half-up).

**Descuento por volumen:** los tramos se muestran como insignias `marca` ("6 un. −10 %"); el descuento aplicado a una línea, como insignia `acento` ("−10 %") junto al precio tachado.

**No**: redondear distinto al backend; mostrar "S/." o sin decimales; usar `fs-color-error` para precios rebajados.
