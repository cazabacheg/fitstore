Botón de acción con variantes primario, contorno, acento, secundario, texto y peligro.

**Úsalo** para cualquier acción. Una sola acción `primario` por vista (la que avanza el flujo: "Agregar al carrito", "Confirmar pedido", "Guardar"); las demás van en `contorno` o `texto`.

**Variantes**
- `primario`: acción principal y botón del hero (`fs-color-primario` con `fs-color-en-primario`).
- `contorno`: acción secundaria sobre `fs-color-superficie`.
- `acento`: acciones de promoción puntuales ("Ver ofertas"); casi nunca hace falta.
- `secundario`: acciones sobre la barra o cabecera oscura.
- `texto`: enlaces de acción en tablas y tarjetas ("Ver ›", "Reprogramar"), en `fs-color-primario-texto`.
- `peligro`: acciones destructivas o irreversibles ("Cancelar pedido", "Marcar devuelto"); siempre piden confirmación con `Dialogo`.

**Lo que da el consumidor:** el texto (verbo + objeto), la variante, el tamaño (`sm` en filas de tabla, `lg` en el checkout y el pago) y, si aplica, un icono a la izquierda.

**No**: dos primarios juntos; un botón solo con icono sin `aria-label`; ocultar una acción que no se puede usar (se deshabilita y se explica por qué).
