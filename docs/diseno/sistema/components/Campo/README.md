Campo de formulario con etiqueta, control (entrada, select o área de texto), ayuda y error.

**Úsalo** en todos los formularios de la tienda y del backoffice. La etiqueta siempre visible, arriba del control; nada de placeholders como única etiqueta.

**Lo que da el consumidor:** la etiqueta, el control (`fs-input`, `fs-select`, `textarea.fs-input` o `fs-check`), un texto de ayuda opcional y el mensaje de error que llega del backend (`errores[].mensaje` de `ApiError`, contracts K5).

**Error:** se marca con `fs-campo--error`, borde de 2px en `fs-color-error`, icono y el mensaje debajo; `aria-invalid` y `aria-describedby` apuntando al mensaje. El error se muestra junto al campo que nombra `errores[].campo`.

**No**: validar solo con color; borrar lo que la persona escribió al fallar; poner el error en un aviso flotante cuando corresponde a un campo.
