Aviso flotante breve que confirma una acción o informa un error general.

**Úsalo** para confirmar ("Guardado", "Agregado al carrito", "Pedido creado") y para errores que no son de un campo (429, 503, sin conexión). Abajo al centro en móvil, abajo a la derecha en escritorio; desaparece a los 5 s (los de error se quedan hasta cerrarlos).

**Lo que da el consumidor:** el tipo (`exito`, `error`, `info`), el título y una línea opcional. Lo dispara el interceptor `errores` de la SPEC-0b (R7) o la funcionalidad.

**Accesibilidad:** `role="status"` para éxito e información, `role="alert"` para errores.

**No**: más de dos avisos a la vez; avisos para errores de campo (van en `Campo`); acciones destructivas dentro del aviso.
