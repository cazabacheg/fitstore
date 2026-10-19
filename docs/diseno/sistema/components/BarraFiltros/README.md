Barra de filtros de los listados del backoffice con buscador, selects y chips de estado con contador.

**Úsala** encima de `Tabla`. Los chips muestran los `contadores` del contrato (K6) sin aplicar el filtro de estado.

**Lo que da el consumidor:** los filtros disponibles y sus valores; la barra los sincroniza con la URL (`?estado=PAGADO&desde=…`) para que el enlace se pueda compartir y "Atrás" funcione.

**Reglas:** el buscador espera 400 ms antes de consultar; "Limpiar filtros" solo aparece si hay alguno activo; el chip activo usa `aria-pressed`.

**No**: un botón "Buscar" (filtra al cambiar); más de 4 controles visibles (el resto en "Más filtros").
