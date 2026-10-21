"""Datos de demostración del catálogo (SPEC-2, sección 6): categorías, productos, variantes, stock e imágenes.

Es la única fuente de verdad de la demo: de aquí salen el SQL (`generar_sql.py`) y las fotos (`semilla_imagenes.py`), así
que los ids, los colores y los nombres de archivo coinciden siempre. Los ids son los rangos de docs/demo/ids-demo.md:
categorías 101-199, productos 1001-1099, variantes 10001-19999 e imágenes 20001-29999.
"""

import re
import unicodedata

# (id, padre, nombre, orden, tiene guía de tallas)
CATEGORIAS = [
    (101, None, "Polos y tops", 0, True),
    (102, 101, "Polos", 0, False),
    (103, 101, "Tops", 1, False),
    (104, None, "Leggings y pantalones", 1, True),
    (105, 104, "Leggings", 0, False),
    (106, 104, "Joggers", 1, False),
    (107, 104, "Pantalones", 2, False),
    (108, 104, "Shorts", 3, False),
    (109, 104, "Faldas", 4, False),
    (110, None, "Casacas y polerones", 2, False),
    (111, None, "Vestidos", 3, True),
    (112, None, "Conjuntos", 4, False),
    (113, None, "Nueva colección", 5, False),
]

GUIAS = {
    101: (["Talla", "Pecho (cm)", "Largo (cm)"], [["S", "86-91", "64"], ["M", "92-97", "66"], ["L", "98-103", "68"], ["XL", "104-109", "70"]]),
    104: (["Talla", "Cintura (cm)", "Cadera (cm)", "Largo (cm)"],
          [["S", "62-66", "88-92", "88"], ["M", "67-71", "93-97", "90"], ["L", "72-76", "98-102", "92"], ["XL", "77-82", "103-108", "94"]]),
    111: (["Talla", "Busto (cm)", "Cintura (cm)", "Largo (cm)"],
          [["S", "84-88", "64-68", "86"], ["M", "89-93", "69-73", "88"], ["L", "94-98", "74-78", "90"]]),
}

# Colores de la semilla V2 (orden de creación = id) más "Variado" (id 11, estampados y surtidos)
COLORES = [
    ("negro", "#1D1D1F"), ("blanco", "#FFFFFF"), ("gris", "#9A9A9A"), ("azul marino", "#1F2A44"), ("azul", "#2563EB"),
    ("rojo", "#DC2626"), ("rosado", "#F472B6"), ("verde", "#16A34A"), ("morado", "#7C3AED"), ("beige", "#D8C7A8"),
    ("Variado", "#A855F7"),
]

# codigo, nombre, descripcion, precio, estado, destacado (orden o None), categorias, forma de la foto, colores, tallas, modo de descuento
PRODUCTOS = [
    (1001, "LEG01", "Legging Pro Compresión", "Legging de compresión media con tiro alto y secado rápido. Ideal para gimnasio y running.",
     "59.90", "ACTIVO", 1, [105], "legging", ["negro", "azul marino", "rojo"], ["S", "M", "L", "XL"], "GLOBAL"),
    (1002, "LEG02", "Legging Seamless Tiro Alto", "Legging sin costuras que se adapta al cuerpo. Tela suave y elástica de cuatro vías.",
     "69.90", "ACTIVO", None, [105, 113], "legging", ["negro", "gris", "rosado"], ["XS", "S", "M", "L"], "GLOBAL"),
    (1003, "LEG03", "Legging Estampado Tropical", "Legging con estampado tropical para entrenar con color. Cintura ancha que no se enrolla.",
     "64.90", "ACTIVO", None, [105], "legging", ["Variado", "negro"], ["S", "M", "L"], "GLOBAL"),
    (1004, "JOG01", "Jogger Algodón Premium", "Jogger de algodón con puños elásticos y bolsillos laterales. Cómodo para el día a día.",
     "79.90", "ACTIVO", 3, [106], "jogger", ["gris", "negro", "beige"], ["S", "M", "L", "XL"], "PERSONALIZADO"),
    (1005, "PAN01", "Pantalón Cargo Deportivo", "Pantalón cargo de tela ligera con bolsillos amplios. Corte recto y cintura ajustable.",
     "89.90", "ACTIVO", None, [107], "pantalon", ["negro", "verde"], ["S", "M", "L", "XL"], "SIN_DESCUENTO"),
    (1006, "SHO01", "Short Running Ligero", "Short ligero con malla interior y bolsillo para llaves. Para correr sin pensar en nada.",
     "34.90", "ACTIVO", 5, [108], "short", ["negro", "azul", "rojo"], ["S", "M", "L"], "GLOBAL"),
    (1007, "FAL01", "Falda Tenis Plisada", "Falda plisada con short interior. Tela elástica y ligera para tenis, padel o paseo.",
     "49.90", "ACTIVO", None, [109], "falda", ["blanco", "negro", "rosado"], ["XS", "S", "M", "L"], "GLOBAL"),
    (1008, "POL01", "Polo Algodón Pima", "Polo de algodón pima peinado, suave y fresco. Corte regular y cuello reforzado.",
     "39.90", "ACTIVO", None, [102], "polo", ["blanco", "negro", "azul marino", "gris"], ["S", "M", "L"], "GLOBAL"),
    (1009, "POL02", "Polo DryFit Entrenamiento", "Polo de secado rápido con paneles de ventilación. Libera el sudor y no se pega al cuerpo.",
     "44.90", "ACTIVO", 4, [102], "polo", ["azul", "rojo", "verde"], ["S", "M", "L", "XL"], "GLOBAL"),
    (1010, "TOP01", "Top Deportivo Soporte Medio", "Top con soporte medio y espalda con tiras cruzadas. Copas removibles.",
     "42.90", "ACTIVO", 2, [103], "top", ["negro", "rosado", "morado"], ["XS", "S", "M", "L"], "GLOBAL"),
    (1011, "TOP02", "Top Cruzado Yoga", "Top cruzado al frente para yoga y pilates. Tela suave tipo mantequilla.",
     "37.90", "ACTIVO", None, [103], "top", ["beige", "negro"], ["XS", "S", "M", "L"], "GLOBAL"),
    (1012, "CAS01", "Casaca Rompeviento", "Casaca ligera rompeviento con capucha plegable y cierre completo. Repele la llovizna.",
     "119.90", "ACTIVO", None, [110], "casaca", ["negro", "azul marino"], ["S", "M", "L", "XL"], "GLOBAL"),
    (1013, "POL03", "Polerón Canguro Felpa", "Polerón de felpa afranelada con capucha y bolsillo canguro. Abrigado y suave.",
     "99.90", "ACTIVO", None, [110], "poleron", ["gris", "negro", "beige"], ["S", "M", "L"], "GLOBAL"),
    (1014, "VES01", "Vestido Deportivo Tenis", "Vestido con short interior y espalda con cortes. Para tenis, golf o salir a caminar.",
     "84.90", "ACTIVO", 6, [111], "vestido", ["negro", "rosado", "azul"], ["S", "M", "L"], "GLOBAL"),
    (1015, "CON01", "Conjunto Top y Legging Fit", "Conjunto de top y legging a juego. Tela de compresión media con efecto realce.",
     "109.90", "ACTIVO", None, [112, 113], "conjunto", ["negro", "morado", "verde"], ["S", "M", "L"], "GLOBAL"),
    (1016, "LEG04", "Legging Térmico Invierno", "Legging con interior afelpado para entrenar en días fríos. En preparación.",
     "74.90", "BORRADOR", None, [105], "legging", ["negro", "gris"], ["S", "M", "L"], "GLOBAL"),
    (1017, "CAS02", "Casaca Corta Crop", "Casaca corta tipo crop con cierre y bolsillos. En preparación.",
     "94.90", "BORRADOR", None, [110], "casaca", ["negro", "rojo"], ["S", "M", "L"], "GLOBAL"),
]

# Tramos propios del producto en modo PERSONALIZADO
TRAMOS_PROPIOS = {1004: [(2, "8"), (4, "12")]}

TALLAS = ["XS", "S", "M", "L", "XL", "XXL"]
# Cantidades que se reparten entre las variantes en orden (cubren agotado, últimas unidades y stock sano)
PATRON_STOCK = [24, 12, 0, 5, 40, 3, 18, 2, 60, 9, 1, 30, 0, 15, 7, 50]
PRODUCTO_AGOTADO = 1011  # sin stock en ninguna variante: la tarjeta sale "Agotado"


def slug(texto):
    s = unicodedata.normalize("NFD", texto).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def prefijo_color(nombre):
    letras = re.sub(r"[^A-Za-z]", "", unicodedata.normalize("NFD", nombre).encode("ascii", "ignore").decode()).upper()
    return letras[:3]


def codigo_color(nombre):
    """Mismas reglas que CodigosSku del servicio: 3 letras; si chocan, el siguiente por id lleva un dígito."""
    mismos = [c[0] for c in COLORES if prefijo_color(c[0]) == prefijo_color(nombre)]
    posicion = mismos.index(nombre)
    return prefijo_color(nombre) if posicion == 0 else prefijo_color(nombre) + str(posicion + 1)


def variantes():
    """[(id, productoId, talla, color, sku, stock fisico, reservado)] con ids desde 10001."""
    salida = []
    siguiente = 10001
    contador = 0
    for pid, codigo, _n, _d, _p, _e, _dest, _c, _f, colores, tallas, _m in PRODUCTOS:
        for color in colores:
            for talla in tallas:
                fisico = 0 if pid == PRODUCTO_AGOTADO else PATRON_STOCK[contador % len(PATRON_STOCK)]
                reservado = 2 if (fisico >= 12 and contador % 9 == 0) else 0
                salida.append((siguiente, pid, talla, color, f"{codigo}-{codigo_color(color)}-{talla}", fisico, reservado))
                siguiente += 1
                contador += 1
    return salida


def imagenes():
    """[(id, productoId, color, clave base, principal, orden)] con ids desde 20001: una foto por producto y color."""
    salida = []
    siguiente = 20001
    for pid, _c, _n, _d, _p, estado, _dest, _cats, _forma, colores, _t, _m in PRODUCTOS:
        if estado != "ACTIVO":
            continue
        for orden, color in enumerate(colores):
            salida.append((siguiente, pid, color, f"productos/{pid}/demo-{slug(color)}", orden == 0, orden))
            siguiente += 1
    return salida


def archivo_foto(producto_id, color):
    """Nombre del JPG en backend/catalog-service/src/main/resources/demo/imagenes/."""
    return f"p{producto_id}-{slug(color)}.jpg"
