Zona de subida de imágenes con arrastrar y soltar, validación de tipo y tamaño, y vista previa.

**Úsala** para fotos de producto (SPEC-2), logos e imágenes de la landing (SPEC-3).

**Lo que da el consumidor:** los tipos aceptados, el tamaño máximo, el máximo de archivos y, para productos, el color de cada foto. Valida en el navegador antes de subir; el backend vuelve a validar el tipo real.

**Reglas:** el borde pasa a `fs-color-primario` mientras se arrastra; cada archivo rechazado muestra su nombre y el motivo; las miniaturas se reordenan arrastrando y se quitan con su botón.

**No**: subir sin vista previa; aceptar SVG (riesgo de scripts).
