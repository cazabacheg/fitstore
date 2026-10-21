"""Dibuja las fotos fuente de las prendas de demostración en infra/scripts/semilla-imagenes/fuentes/.

Son ilustraciones propias del proyecto (obra original, sin derechos de terceros): una prenda gris con sombras suaves sobre
fondo blanco, pensada para que `semilla_imagenes.py` la recoloree. Se pueden reemplazar por fotos reales con licencia libre
(Unsplash o Pexels) conservando el nombre del archivo: el recoloreo solo necesita prenda sobre fondo blanco.

Uso (con el venv del requirements.txt):  python3 infra/scripts/semilla-imagenes/generar_fuentes.py
"""

import os

from PIL import Image, ImageDraw, ImageFilter

LADO = 1200
BASE = 150  # gris de la prenda; el recoloreo lo toma como "luminosidad 1.0"
RAIZ = os.path.dirname(os.path.abspath(__file__))

# Siluetas en coordenadas 0-100 (x, y)
FORMAS = {
    "polo": [[(36, 12), (44, 17), (56, 17), (64, 12), (82, 19), (95, 37), (83, 46), (77, 39), (77, 88), (23, 88), (23, 39), (17, 46),
              (5, 37), (18, 19)]],
    "top": [[(34, 10), (40, 10), (43, 30), (57, 30), (60, 10), (66, 10), (70, 38), (72, 62), (28, 62), (30, 38)]],
    "legging": [[(30, 8), (70, 8), (72, 22), (66, 95), (53, 95), (50, 36), (47, 95), (34, 95), (28, 22)]],
    "jogger": [[(28, 8), (72, 8), (77, 28), (72, 90), (72, 95), (53, 95), (53, 90), (50, 38), (47, 90), (47, 95), (28, 95), (28, 90), (23, 28)]],
    "pantalon": [[(30, 8), (70, 8), (76, 92), (53, 92), (50, 38), (47, 92), (24, 92)]],
    "short": [[(27, 20), (73, 20), (79, 64), (54, 64), (50, 42), (46, 64), (21, 64)]],
    "falda": [[(34, 18), (66, 18), (80, 76), (20, 76)]],
    "casaca": [[(34, 8), (44, 13), (56, 13), (66, 8), (86, 20), (96, 72), (84, 74), (77, 42), (77, 90), (23, 90), (23, 42), (16, 74), (4, 72),
                (14, 20)]],
    "poleron": [[(34, 14), (44, 18), (56, 18), (66, 14), (86, 26), (96, 76), (84, 78), (77, 46), (77, 92), (23, 92), (23, 46), (16, 78),
                 (4, 76), (14, 26)]],
    "vestido": [[(38, 8), (44, 13), (56, 13), (62, 8), (67, 28), (61, 40), (76, 94), (24, 94), (39, 40), (33, 28)]],
    "conjunto": [[(35, 5), (40, 5), (43, 18), (57, 18), (60, 5), (65, 5), (68, 24), (69, 36), (31, 36), (32, 24)],
                 [(32, 40), (68, 40), (70, 50), (65, 96), (53, 96), (50, 58), (47, 96), (35, 96), (30, 50)]],
}


def punto(p):
    return (p[0] * LADO / 100, p[1] * LADO / 100)


def dibujar(nombre, poligonos):
    mascara = Image.new("L", (LADO, LADO), 0)
    trazo = ImageDraw.Draw(mascara)
    for poligono in poligonos:
        trazo.polygon([punto(p) for p in poligono], fill=255)
    suave = mascara.filter(ImageFilter.GaussianBlur(1.2))

    # Relieve: más claro al centro y más oscuro hacia los bordes de la prenda, con un degradado vertical suave
    interior = mascara.filter(ImageFilter.GaussianBlur(26))
    prenda = Image.new("L", (LADO, LADO), BASE)
    pix = prenda.load()
    borde = interior.load()
    for y in range(LADO):
        degradado = 1.08 - 0.16 * (y / LADO)
        for x in range(LADO):
            relieve = 0.78 + 0.30 * (borde[x, y] / 255.0)
            pix[x, y] = max(0, min(255, int(BASE * degradado * relieve)))
    trazos = ImageDraw.Draw(prenda)
    # Costuras y pliegues
    for poligono in poligonos:
        centro = sum(p[0] for p in poligono) / len(poligono)
        trazos.line([punto((centro, poligono[0][1] + 4)), punto((centro, poligono[0][1] + 24))], fill=int(BASE * 0.7), width=4)
    if nombre in ("casaca", "poleron"):
        trazos.line([punto((50, 14)), punto((50, 90))], fill=int(BASE * 0.55), width=5)
    if nombre == "poleron":
        trazos.rectangle([punto((34, 62)), punto((66, 78))], outline=int(BASE * 0.6), width=4)
    if nombre in ("legging", "jogger", "pantalon", "short"):
        trazos.line([punto((30, 14)), punto((70, 14))], fill=int(BASE * 0.6), width=6)

    sombra = mascara.filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.10))
    lienzo = Image.new("RGB", (LADO, LADO), (255, 255, 255))
    lienzo.paste((0, 0, 0), (10, 14), sombra)
    lienzo.paste(Image.merge("RGB", (prenda, prenda, prenda)), (0, 0), suave)
    return lienzo


def main():
    destino = os.path.join(RAIZ, "fuentes")
    os.makedirs(destino, exist_ok=True)
    for nombre, poligonos in FORMAS.items():
        # PNG de 8 bits con paleta reducida: pocos KB por prenda
        dibujar(nombre, poligonos).convert("P", palette=Image.ADAPTIVE, colors=128).save(
            os.path.join(destino, nombre + ".png"), optimize=True)
        print("fuente", nombre)


if __name__ == "__main__":
    main()
