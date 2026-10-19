FitStore es una plantilla de tienda en línea para MYPEs de ropa deportiva de Gamarra. Cada tienda pone su marca desde el backoffice (cinco colores y el logo) sin recompilar. Este sistema define cómo se ve cualquier tienda hecha con FitStore: los tokens que la marca configura, los que el frontend deriva de ella y los componentes compartidos de la tienda (Angular + Tailwind 4) y del backoffice (Angular Material 3).

Los dos temas de este sistema son dos marcas: **FitStore**, la plantilla por defecto, y **Kallpa**, una marca de prueba de una MYPE de Gamarra que sirve para demostrar el white-label. Cambia de tema en la página para ver cada componente con la otra marca.

## Contenido y tono

- Español de Perú, tuteo y frases cortas: "Agrega 2 más y ahorra 15 %", "Apartamos tus prendas mientras completas el pago".
- Mayúscula solo al inicio de la frase, también en botones y títulos: "Ir al checkout", "Registrar reembolso".
- Los botones dicen exactamente lo que hacen: "Confirmar pedido", "Marcar devuelto". Después, un aviso que confirma: "Pedido creado".
- Los errores dicen qué pasó y qué hacer, sin disculpas: "Los precios cambiaron, revisa tu pedido", "Solo quedan 3 unidades".
- Montos con "S/ " y dos decimales (`S/ 59.90`); fechas en es-PE ("jueves 22 de octubre"); códigos en `codigo` (`FS-000124`, `LEG01-NEG-M`).
- Sin emoji en la interfaz. La única excepción es la franja de beneficios de la landing, cuyo icono escribe el admin.

## Marca configurable (white-label)

- La tienda guarda cinco colores (`fs-color-primario`, `fs-color-secundario`, `fs-color-acento`, `fs-color-fondo`, `fs-color-texto`). La tipografía y los radios son fijos: todas las tiendas tienen los mismos componentes y solo cambian los colores. El frontend los escribe como variables CSS en `:root` al cargar `/settings/public` (SPEC-0b R4).
- Los demás colores de marca se **derivan**: `fs-color-en-primario`, `fs-color-en-secundario` y `fs-color-en-acento` eligen blanco o `fs-color-texto` según el contraste; `fs-color-primario-texto` oscurece el primario hasta que se lee como texto sobre el fondo; `fs-color-primario-suave`, `fs-color-superficie-alterna`, `fs-color-texto-suave`, `fs-color-linea` y `fs-color-borde-control` se calculan desde el fondo y el texto. Ningún componente usa un color literal.
- Los colores semánticos (`fs-color-exito`, `fs-color-aviso`, `fs-color-error`, `fs-color-info` y sus `-suave`) son iguales en todas las marcas, para que "Vencido" o "Pago rechazado" se lean igual en cualquier tienda.
- La plantilla FitStore es cálida y sobria: arena `#C9B28C` como primario, con texto oscuro encima (7.7:1); tostado `#7A6343` como su versión para texto; espresso `#2B2621` como secundario; olivo `#4E5B45` como acento y un fondo blanco roto. Cada tienda la reemplaza por su marca.
- Un primario claro como el arena es válido: el frontend deriva `fs-color-en-primario` oscuro y `fs-color-primario-texto` más profundo. Solo cuando un color no alcanza el contraste ni así, la configuración de tienda muestra el aviso (SPEC-3 CA-3-10).

## Color

- Fondo de página en `fs-color-fondo`; tarjetas, tablas y diálogos en `fs-color-superficie`; bandas y cabeceras de tabla en `fs-color-superficie-alterna`.
- Texto en `fs-color-texto`; metadatos y ayudas en `fs-color-texto-suave`.
- Una sola acción principal por vista en `fs-color-primario` con texto `fs-color-en-primario` (también el botón del hero). Las secundarias van con contorno.
- Enlaces, botones de texto, foco, casillas, chips activos e iconos de marca en `fs-color-primario-texto`; nunca `fs-color-primario` como texto.
- `fs-color-acento` es solo relleno (insignias de descuento, destacados) con texto `fs-color-en-acento`.
- `fs-color-secundario` pinta el pie de la tienda, el fondo del hero mientras carga la imagen y la barra lateral del backoffice. Ahí el ítem activo puede ir en `fs-color-primario` (en FitStore, arena sobre espresso da 7.3:1).
- La cabecera de la tienda es clara: `fs-color-superficie` con una línea inferior en `fs-color-linea`. El menú muestra hasta 4 categorías en `etiqueta` (mayúsculas, letra espaciada); la de la página actual va subrayada en `fs-color-texto` y, al pasar el mouse o el foco por una, las demás bajan al 40 % de opacidad. En móvil el menú (☰) va pegado al logo.
- Un estado nunca se comunica solo con color: la insignia lleva la palabra ("Pagado", "Vencido") y, si cabe, un icono.

## Tipografía

- Poppins en todas las tiendas (no se configura). Los estilos de `Marca` usan Poppins.
- `display` solo en el hero; `titulo-1` para el título de cada página; `titulo-2` y `titulo-3` para secciones y tarjetas; `cuerpo` para el texto; `cuerpo-sm` para ayudas y metadatos; `etiqueta` en mayúsculas para insignias y cabeceras de tabla.
- Precios en `precio` con cifras tabulares. Un precio rebajado muestra el original tachado en `cuerpo-sm` y `fs-color-texto-suave`.
- Códigos de pedido, SKU, boletas y seguimiento en `codigo` (JetBrains Mono).

## Espacio, radios y sombras

- Escala de `espacio-1` (4px) a `espacio-16` (64px). Margen lateral mínimo `espacio-4` en móvil.
- Radios fijos en todas las tiendas: `radio-sm` para insignias y casillas, `radio-md` para botones, campos y tarjetas, `radio-lg` para diálogos y el hero. `radio-pill` solo para muestras de color y contadores.
- `sombra-1` para tarjetas al pasar el mouse y menús; `sombra-2` para diálogos y avisos. Las tarjetas en reposo usan borde `fs-color-linea`, sin sombra.

## Estados e interacción

- Foco visible en todo control: anillo de 2px en `fs-color-foco` (`fs-color-primario-texto`) separado 2px por el fondo.
- Deshabilitado: 45 % de opacidad y cursor normal; nunca se oculta una acción que el usuario espera, se deshabilita con su motivo.
- Cada pantalla tiene sus estados de cargando (esqueleto), vacío (qué falta y cómo empezar) y error (qué pasó y "Reintentar"), con el componente `Estados`.
- Las tablas del backoffice siguen la regla R11: el identificador es un enlace, toda la fila es clicable y la última columna lleva "Ver ›", hasta dos botones o el menú "⋮".

## Diseño de página

- Tienda: mobile-first, contenido hasta 1280px, rejilla de 12 columnas en escritorio y 4 en móvil. El pie queda al final del documento y no se ve en la pantalla inicial.
- Backoffice: barra lateral de 240px en `fs-color-secundario`, barra superior con migas y menú de usuario, y el contenido en una columna de hasta 1440px. Usable en tablet.
- Imágenes de producto en proporción 4:5, siempre en `radio-md`.

## Iconografía

- Material Symbols Rounded (Google Fonts), peso 400, sin relleno, a 20px junto a texto y a 24px solos. En el backoffice es el mismo set que usa Angular Material.
- Iconos frecuentes: `shopping_bag` (carrito), `local_shipping` (envío), `storefront` (recojo), `inventory_2` (inventario), `receipt_long` (boleta), `undo` (reembolso), `assignment_return` (devuelto).
- No hay logo de FitStore todavía: el nombre de la tienda se escribe en `titulo-3` con su tipografía. Cada tienda sube su propio logo en la configuración.
