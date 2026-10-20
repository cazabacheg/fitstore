-- Textos predeterminados de las 18 plantillas (micro notification 4.1; SPEC-7 sección 6).
-- Los textos usan solo variables que salen de los campos del evento o de la marca ({{nombreTienda}}).
-- El mensaje admite saltos de línea y **negrita** (SPEC-7 N4). Los opcionales quedan activos; los obligatorios
-- (recuperar contraseña, invitación de staff, pedido confirmado y Libro de Reclamaciones) no se pueden desactivar.
INSERT INTO plantilla (codigo, asunto, titulo, mensaje, texto_boton, activa, obligatoria, actualizada_en) VALUES

-- Al cliente
('BIENVENIDA',
 'Te damos la bienvenida a {{nombreTienda}}, {{nombre}}',
 'Qué gusto tenerte con nosotros',
 E'Hola {{nombre}}, tu cuenta en {{nombreTienda}} ya está lista.\nCuando quieras, entra a la tienda y encuentra tu próxima prenda deportiva.',
 NULL, true, false, now()),

('RECUPERAR_PASSWORD',
 'Recupera tu contraseña de {{nombreTienda}}',
 'Recupera tu contraseña',
 E'Hola {{nombre}}, recibimos una solicitud para cambiar tu contraseña.\nHaz clic en el botón para crear una nueva. El enlace **vence en 30 minutos**.\nSi no fuiste tú, ignora este correo: tu contraseña seguirá igual.',
 'Restablecer contraseña', true, true, now()),

('INVITACION_STAFF',
 'Te invitaron al backoffice de {{nombreTienda}}',
 'Te invitaron al backoffice',
 E'Hola {{nombre}}, te invitaron a administrar {{nombreTienda}}.\nHaz clic en el botón para crear tu contraseña. El enlace **vence en 24 horas**.',
 'Aceptar invitación', true, true, now()),

('PASSWORD_CAMBIADO',
 'Tu contraseña de {{nombreTienda}} cambió',
 'Tu contraseña cambió',
 E'Hola {{nombre}}, te avisamos que la contraseña de tu cuenta se cambió hace un momento.\nSi no fuiste tú, comunícate con nosotros cuanto antes.',
 NULL, true, false, now()),

('PEDIDO_CONFIRMADO',
 '¡Gracias por tu compra, {{nombre}}! Pedido {{codigoPedido}}',
 '¡Pedido confirmado!',
 E'Hola {{nombre}}, recibimos tu pago de **{{total}}**. Te avisaremos cuando tu pedido salga.\nModalidad de entrega: {{modalidad}}.\nAdjuntamos tu boleta.',
 'Ver mi pedido', true, true, now()),

('PEDIDO_CANCELADO',
 'Tu pedido {{codigoPedido}} fue cancelado',
 'Pedido cancelado',
 E'Hola {{nombre}}, tu pedido **{{codigoPedido}}** fue cancelado.\nSi ya lo habías pagado, procesaremos tu reembolso y te avisaremos cuando esté listo.',
 'Ver mi pedido', true, false, now()),

('REEMBOLSO_REALIZADO',
 'Te devolvimos tu dinero del pedido {{codigoPedido}}',
 'Reembolso realizado',
 E'Hola {{nombre}}, realizamos el reembolso de **{{monto}}** de tu pedido **{{codigoPedido}}**.\nEl dinero vuelve al mismo medio con el que pagaste; según tu banco, puede tardar unos días en reflejarse.',
 'Ver mi pedido', true, false, now()),

('EN_CAMINO',
 'Tu pedido {{codigoPedido}} va en camino',
 'Tu pedido va en camino',
 E'Hola {{nombre}}, tu pedido **{{codigoPedido}}** ya salió de la tienda.\nAquí abajo encuentras los datos para seguirlo.',
 'Ver mi pedido', true, false, now()),

('ENTREGA_FALLIDA',
 'No pudimos entregar tu pedido {{codigoPedido}}',
 'No pudimos entregar tu pedido',
 E'Hola {{nombre}}, intentamos entregar tu pedido **{{codigoPedido}}** pero no fue posible.\nTe contactaremos para reprogramar la entrega.',
 NULL, true, false, now()),

('LISTO_PARA_RECOGER',
 'Tu pedido {{codigoPedido}} está listo para recoger',
 'Tu pedido está listo para recoger',
 E'Hola {{nombre}}, tu pedido **{{codigoPedido}}** ya está listo para recoger.\nAquí abajo encuentras dónde y cuándo. Lleva tu DNI.',
 'Ver mi pedido', true, false, now()),

('ENTREGADO',
 '¡Tu pedido {{codigoPedido}} fue entregado!',
 '¡Pedido entregado!',
 E'Hola {{nombre}}, confirmamos que tu pedido **{{codigoPedido}}** fue entregado.\nGracias por comprar en {{nombreTienda}}; esperamos que lo disfrutes.',
 NULL, true, false, now()),

('RECLAMO_COPIA',
 'Copia de tu reclamo {{numeroReclamo}}',
 'Recibimos tu reclamo',
 E'Hola {{nombre}}, registramos tu hoja de reclamación **{{numeroReclamo}}**.\nTe responderemos a más tardar el **{{fechaLimite}}**. Abajo va una copia con todos los datos que registraste.',
 'Ver mi reclamo', true, true, now()),

('RECLAMO_RESPUESTA',
 'Respuesta a tu reclamo {{numeroReclamo}}',
 'Respondimos tu reclamo',
 E'Hola {{nombre}}, ya respondimos tu reclamo **{{numeroReclamo}}**.\nPuedes leer la respuesta completa aquí abajo.',
 'Ver mi reclamo', true, true, now()),

('PEDIDO_DEVUELTO',
 'Tu pedido {{codigoPedido}} regresó a la tienda',
 'Tu pedido regresó a la tienda',
 E'Hola {{nombre}}, tu pedido **{{codigoPedido}}** regresó a {{nombreTienda}} porque no pudimos entregártelo.\nNos comunicaremos contigo para coordinar la devolución de tu dinero.',
 NULL, true, false, now()),

-- A la tienda
('ALERTA_STOCK_BAJO',
 'Stock bajo: {{producto}} ({{talla}}, {{color}})',
 'Stock bajo',
 E'La variante **{{sku}}** ({{producto}}, talla {{talla}}, color {{color}}) tiene **{{disponible}}** unidades disponibles; el mínimo es {{minimo}}.',
 NULL, true, false, now()),

('ALERTA_SIN_STOCK',
 'Pedido {{codigoPedido}} pagado sin stock disponible',
 'Pedido pagado sin stock',
 E'El pedido **{{codigoPedido}}** ya fue pagado, pero no hay stock suficiente para confirmarlo.\nRevísalo en el backoffice para contactar al cliente o reponer el producto.',
 NULL, true, false, now()),

('ALERTA_REEMBOLSO_FALLIDO',
 'Reembolso fallido del pedido {{codigoPedido}}',
 'Reembolso fallido',
 E'No se pudo reembolsar **{{monto}}** del pedido **{{codigoPedido}}**.\nÚltimo error: {{ultimoError}}.\nReintenta el reembolso desde el backoffice.',
 NULL, true, false, now()),

('ALERTA_RECLAMO_NUEVO',
 'Nuevo reclamo {{numeroReclamo}}',
 'Nuevo reclamo',
 E'Se registró el reclamo **{{numeroReclamo}}** (tipo: {{tipo}}).\nLa fecha límite de respuesta es el **{{fechaLimite}}**.',
 NULL, true, false, now());
