"""Genera las fotos de demostración del catálogo: una JPG por producto y color (1200 px, calidad 80).

Para cada producto de `catalogo_demo.py` toma su foto fuente (carpeta `fuentes/`), separa la prenda del fondo blanco y la
recolorea manteniendo las sombras y el fondo. Deja los archivos en
`backend/catalog-service/src/main/resources/demo/imagenes/`, donde catalog los sube a Garage al arrancar con los perfiles
dev o demo. Falla si fuentes + resultados pesan 10 MB o más.

Uso (desde la raíz del repo, con el venv del requirements.txt):  python3 infra/scripts/semilla-imagenes/semilla_imagenes.py
"""

import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import catalogo_demo as demo  # noqa: E402

RAIZ = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(RAIZ, "..", "..", "..", "backend", "catalog-service", "src", "main", "resources", "demo", "imagenes")
LIMITE_BYTES = 10 * 1024 * 1024
LADO = 1200
CALIDAD = 80
BASE = 150.0  # gris de la prenda en las fuentes (luminosidad 1.0)


def hex_a_rgb(hex_):
    return np.array([int(hex_[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float32)


def recolorear(fuente, rgb_destino, variado=False):
    """Prenda recoloreada: color destino x luminosidad de la fuente; el fondo (blanco y sombra) se conserva."""
    foto = np.asarray(fuente.convert("RGB").resize((LADO, LADO), Image.LANCZOS), dtype=np.float32)
    luz = foto.mean(axis=2)
    # Prenda = lo que se aparta del blanco con color de prenda (gris ~BASE); la sombra del fondo es mucho más clara
    alfa = np.clip((235.0 - luz) / 40.0, 0.0, 1.0)[..., None]
    relieve = np.clip(luz / BASE, 0.35, 1.25)[..., None]
    if variado:
        # Estampado: franjas diagonales de varios colores sobre el relieve de la prenda
        yy, xx = np.mgrid[0:LADO, 0:LADO]
        paleta = np.array([[168, 85, 247], [244, 114, 182], [37, 99, 235], [22, 163, 74], [250, 204, 21]], dtype=np.float32)
        indice = (((xx + yy) // 120) % len(paleta)).astype(int)
        color = paleta[indice]
    else:
        color = rgb_destino[None, None, :]
    if rgb_destino.mean() > 235 and not variado:
        # Blanco: la luz solo oscurece (sombras), nunca "quema" el relieve
        prenda = np.clip(color * np.minimum(relieve, 1.0) ** 0.8, 0, 255)
    else:
        prenda = np.clip(color * relieve, 0, 255)
    resultado = alfa * prenda + (1.0 - alfa) * foto
    return Image.fromarray(resultado.astype(np.uint8), "RGB")


def peso_carpeta(ruta):
    total = 0
    for base, _dirs, archivos in os.walk(ruta):
        total += sum(os.path.getsize(os.path.join(base, a)) for a in archivos)
    return total


def main():
    salida = os.path.abspath(SALIDA)
    os.makedirs(salida, exist_ok=True)
    colores = {nombre: hex_ for nombre, hex_ in demo.COLORES}
    generadas = 0
    for pid, _c, _n, _d, _p, estado, _dest, _cats, forma, lista_colores, _t, _m in demo.PRODUCTOS:
        if estado != "ACTIVO":
            continue
        fuente = Image.open(os.path.join(RAIZ, "fuentes", forma + ".png"))
        for color in lista_colores:
            foto = recolorear(fuente, hex_a_rgb(colores[color]), variado=(color == "Variado"))
            foto.save(os.path.join(salida, demo.archivo_foto(pid, color)), "JPEG", quality=CALIDAD, optimize=True, progressive=True)
            generadas += 1
    total = peso_carpeta(salida) + peso_carpeta(os.path.join(RAIZ, "fuentes"))
    print(f"{generadas} fotos en {salida}; fuentes + resultados: {total / 1024 / 1024:.2f} MB")
    if total >= LIMITE_BYTES:
        sys.exit("Fuentes + resultados superan los 10 MB: baja la calidad o quita colores")


if __name__ == "__main__":
    main()
