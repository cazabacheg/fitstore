# Créditos de las imágenes de demostración

Las imágenes de demostración del catálogo (SPEC-2, sección 6) se generan con estos scripts; aquí se anota su origen.

## Fuentes actuales

Las 11 imágenes fuente de `fuentes/` (`polo`, `top`, `legging`, `jogger`, `pantalon`, `short`, `falda`, `casaca`, `poleron`,
`vestido`, `conjunto`) son **ilustraciones propias del proyecto**, dibujadas por `generar_fuentes.py`: obra original, sin
derechos de terceros ni atribución requerida. Son prendas grises sobre fondo blanco con sombras suaves.

| Fuente | Autor | Licencia | Origen |
|---|---|---|---|
| `fuentes/*.png` | Equipo FitStore (script `generar_fuentes.py`) | Obra propia del proyecto | Generadas, no descargadas |

## Fotos reales (pendiente de decisión del autor)

La spec pide fotos de bancos con licencia libre (Unsplash o Pexels). Al preparar esta entrega no se encontraron fotos de
prendas deportivas sobre fondo blanco con licencia verificable para todas las formas, así que no se incluyó ninguna foto de
terceros. Para usarlas basta reemplazar un archivo de `fuentes/` por una foto de la prenda sobre fondo blanco con el mismo
nombre, volver a correr `semilla_imagenes.py` y anotar aquí, por cada archivo: autor, URL de la foto y licencia
(Unsplash License o Pexels License, que permiten modificar y usar comercialmente sin atribución obligatoria).

## Cómo se generan

```bash
python3 -m venv .venv && . .venv/bin/activate && pip install -r infra/scripts/semilla-imagenes/requirements.txt
python3 infra/scripts/semilla-imagenes/generar_fuentes.py      # solo si se cambian las siluetas
python3 infra/scripts/semilla-imagenes/semilla_imagenes.py     # recolorea y deja las JPG de 1200 px (calidad 80)
python3 infra/scripts/semilla-imagenes/generar_sql.py          # regenera R__catalogo_demo.sql (ids, SKU, stock)
```

`semilla_imagenes.py` separa la prenda del fondo blanco, la recolorea conservando las sombras y deja una JPG por producto y
color en `backend/catalog-service/src/main/resources/demo/imagenes/` (`p{productoId}-{color}.jpg`). Falla si fuentes y
resultados pesan 10 MB o más (hoy, alrededor de 1.4 MB).
