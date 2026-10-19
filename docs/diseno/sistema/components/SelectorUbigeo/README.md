Selector de ubigeo en tres niveles con buscador de distritos, que emite el código INEI.

**Úsalo** en direcciones (SPEC-1), la tienda física (SPEC-3) y las tarifas de envío (SPEC-6).

**Lo que da el consumidor:** el código inicial (opcional) y la opción `hasta` = `departamento`, `provincia` o `distrito` (por defecto). Con `hasta = departamento` se detiene en el primer select y emite 2 dígitos (SPEC-0b CA-0b-26).

**Comportamiento:** las listas vienen de `/ubigeo` con caché en el navegador; elegir un departamento vacía provincia y distrito; el buscador ("miraf") encuentra el distrito sin importar tildes y llena los tres selects. En móvil los tres selects se apilan.

**No**: pedir el código a la persona; permitir distrito sin provincia.
