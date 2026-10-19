Estados comunes de una vista: cargando (esqueleto), vacío y error (SPEC-0b P-0b-03).

**Úsalos** en toda pantalla que cargue datos. El esqueleto imita la forma de lo que viene (tarjetas, filas); el vacío dice qué falta y cómo empezar; el error dice qué pasó y ofrece "Reintentar".

**Lo que da el consumidor:** el tipo de estado, el título, el texto y la acción (texto y destino).

**Servicio no disponible (503):** usa el estado de error con "Inténtalo en un momento" y no bloquea el resto de la página (SPEC-8 CA-8-04).

**No**: spinners a pantalla completa; vacíos sin acción; mensajes técnicos ("Error 500").
