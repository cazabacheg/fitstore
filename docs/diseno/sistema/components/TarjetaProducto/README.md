Tarjeta de producto para el listado, los destacados de la landing y "Productos relacionados".

**Úsala** en rejillas de 2 columnas en móvil y 4 en escritorio.

**Lo que da el consumidor:** el `ProductoTarjeta` del contrato de catalog (nombre, slug, `imagenUrl` de 400px, `precioDesde`, `colores`, `etiquetaDescuento`, `agotado`). Toda la tarjeta es un enlace a `/producto/:slug`.

**Reglas:** imagen 4:5 con `radio-md`; la etiqueta de descuento arriba a la izquierda en `acento`; agotado = imagen atenuada e insignia neutra "Agotado"; muestras de color en `radio-pill` con nombre accesible. Las muestras de la vista previa usan tokens de ejemplo; en la tienda son el `hex` de cada color.

**No**: botón "Agregar al carrito" en la tarjeta (se elige talla y color en el detalle); más de una insignia.
