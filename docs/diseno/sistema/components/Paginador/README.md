Paginador numérico para el listado de la tienda y "Mis pedidos".

**Úsalo** cuando hay más de una página (`totalPaginas` > 1 del contrato, K6). En el backoffice va dentro del pie de `Tabla`.

**Lo que da el consumidor:** `pagina` (desde 0), `totalPaginas` y `totalElementos`; emite la nueva página. La página va en la URL (`?pagina=2`) para que funcione "Atrás".

**Reglas:** máximo 7 botones con elipsis; la página actual con `aria-current="page"`; al cambiar de página, el foco vuelve al inicio de la lista.

**No**: scroll infinito (el pie no se alcanzaría).
