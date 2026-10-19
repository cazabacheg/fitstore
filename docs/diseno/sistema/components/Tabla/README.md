Tabla paginada del backoffice con la convención de filas R11 (SPEC-0b).

**Úsala** en todos los listados del backoffice: pedidos, productos, inventario, pagos, entregas, clientes, reclamos.

**Lo que da el consumidor:** las columnas, la página del contrato (`contenido`, `pagina`, `tamano`, `totalElementos`), el identificador de cada fila y su destino, y las acciones de la fila. En Angular es `FsTabla` sobre `mat-table` con `filaEnlace` y `acciones`.

**Reglas R11:** el identificador (código, nombre o número) es un enlace real (Tab, Enter, Ctrl+clic); toda la fila es clicable con cambio de color al pasar el mouse; la última columna lleva "Ver ›", hasta 2 botones de `texto` o el menú "⋮", y sus clics no abren el detalle. Filas por vencer en `fs-color-aviso-suave`, vencidas o agotadas en `fs-color-error-suave`. Códigos en `codigo`, montos alineados con cifras tabulares.

**No**: acciones fuera de la última columna; filas sin detalle que parezcan clicables (movimientos del kardex, errores de carga).
