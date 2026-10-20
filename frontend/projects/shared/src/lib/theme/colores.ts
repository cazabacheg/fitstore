/**
 * Colores de la marca y derivados (docs/diseno/sistema/README.md, "Marca configurable").
 * La tienda configura cinco colores; el resto se calcula aquí para que cualquier marca cumpla los contrastes.
 */

/** Los cinco colores que guarda settings (`Tema`). */
export interface Tema {
  colorPrimario: string;
  colorSecundario: string;
  colorAcento: string;
  colorFondo: string;
  colorTexto: string;
}

/** Colores que el frontend deriva de los cinco configurables. */
export interface ColoresDerivados {
  /** Texto sobre el primario (blanco o el texto, el de más contraste). */
  enPrimario: string;
  enSecundario: string;
  enAcento: string;
  /** El primario oscurecido hasta 4.5:1 sobre el fondo: enlaces, foco, casillas. */
  primarioTexto: string;
  /** Tinte del primario: franjas, tramos de descuento, fila seleccionada. */
  primarioSuave: string;
  superficieAlterna: string;
  /** Metadatos y ayudas: ≥ 5.5:1 sobre fondo, superficie y superficie alterna. */
  textoSuave: string;
  linea: string;
  /** Borde de campos y casillas: ≥ 3:1 sobre fondo y superficie. */
  bordeControl: string;
}

export type Rgb = [number, number, number];

/** Tema de la plantilla FitStore (tokens.json): el que se usa sin configuración. */
export const TEMA_FITSTORE: Tema = {
  colorPrimario: '#C9B28C',
  colorSecundario: '#2B2621',
  colorAcento: '#4E5B45',
  colorFondo: '#FAF9F7',
  colorTexto: '#26221D',
};

/** Derivados de FitStore tal como los fijó el diseño (tokens.json). */
export const COLORES_FITSTORE: ColoresDerivados = {
  enPrimario: '#26221D',
  enSecundario: '#FFFFFF',
  enAcento: '#FFFFFF',
  primarioTexto: '#7A6343',
  primarioSuave: '#F3EDE3',
  superficieAlterna: '#F2F0EC',
  textoSuave: '#6A6259',
  linea: '#E6E1D9',
  bordeControl: '#8A8277',
};

const BLANCO = '#FFFFFF';

export function hexARgb(hex: string): Rgb {
  const limpio = hex.replace('#', '');
  return [0, 2, 4].map((i) => parseInt(limpio.slice(i, i + 2), 16)) as Rgb;
}

export function rgbAHex([r, g, b]: Rgb): string {
  const par = (v: number) =>
    Math.min(255, Math.max(0, Math.round(v)))
      .toString(16)
      .padStart(2, '0');
  return `#${par(r)}${par(g)}${par(b)}`.toUpperCase();
}

function luminancia(hex: string): number {
  const [r, g, b] = hexARgb(hex).map((canal) => {
    const c = canal / 255;
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** Contraste WCAG entre dos colores (1 a 21). */
export function contraste(a: string, b: string): number {
  const [x, y] = [luminancia(a), luminancia(b)];
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
}

/** Mezcla `a` hacia `b`: t = 0 devuelve `a` y t = 1 devuelve `b`. */
export function mezclar(a: string, b: string, t: number): string {
  const [x, y] = [hexARgb(a), hexARgb(b)];
  return rgbAHex(x.map((v, i) => v + (y[i] - v) * t) as Rgb);
}

function rgbAHsl([r, g, b]: Rgb): [number, number, number] {
  const [rn, gn, bn] = [r / 255, g / 255, b / 255];
  const max = Math.max(rn, gn, bn);
  const min = Math.min(rn, gn, bn);
  const l = (max + min) / 2;
  const d = max - min;
  if (d === 0) return [0, 0, l];
  const s = d / (1 - Math.abs(2 * l - 1));
  let h: number;
  if (max === rn) h = ((gn - bn) / d) % 6;
  else if (max === gn) h = (bn - rn) / d + 2;
  else h = (rn - gn) / d + 4;
  return [(h * 60 + 360) % 360, s, l];
}

function hslARgb([h, s, l]: [number, number, number]): Rgb {
  const c = (1 - Math.abs(2 * l - 1)) * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = l - c / 2;
  const [r, g, b] =
    h < 60
      ? [c, x, 0]
      : h < 120
        ? [x, c, 0]
        : h < 180
          ? [0, c, x]
          : h < 240
            ? [0, x, c]
            : h < 300
              ? [x, 0, c]
              : [c, 0, x];
  return [(r + m) * 255, (g + m) * 255, (b + m) * 255];
}

/** Blanco o el color de texto, el que dé más contraste sobre `fondo`. */
function textoSobre(fondo: string, texto: string): string {
  return contraste(BLANCO, fondo) >= contraste(texto, fondo) ? BLANCO : rgbAHex(hexARgb(texto));
}

/** Oscurece `color` (conservando su tono) hasta que alcance 4.5:1 sobre `fondo`. */
function oscurecerParaTexto(color: string, fondo: string): string {
  const original = rgbAHex(hexARgb(color));
  if (contraste(original, fondo) >= 4.5) return original;
  const [h, s, l] = rgbAHsl(hexARgb(color));
  for (let luz = l - 0.005; luz > 0; luz -= 0.005) {
    const candidato = rgbAHex(hslARgb([h, s, luz]));
    if (contraste(candidato, fondo) >= 4.5) return candidato;
  }
  return '#000000';
}

/** Mezcla `fondo` hacia `texto` desde `inicio` hasta que `cumple` acepte el resultado. */
function mezclaHastaCumplir(
  fondo: string,
  texto: string,
  inicio: number,
  cumple: (c: string) => boolean,
): string {
  let color = mezclar(fondo, texto, inicio);
  for (let t = inicio + 0.01; !cumple(color) && t <= 1; t += 0.01) color = mezclar(fondo, texto, t);
  return color;
}

/**
 * Deriva los colores con las fórmulas del sistema de diseño. Las mezclas se calibraron contra FitStore y Kallpa
 * (tokens.json): las mezclas dan valores muy cercanos a los del diseño, y los contrastes se garantizan siempre.
 */
export function derivarPorFormula(tema: Tema): ColoresDerivados {
  const { colorPrimario, colorSecundario, colorAcento, colorFondo, colorTexto } = tema;
  const superficieAlterna = mezclar(colorFondo, colorTexto, 0.035);
  return {
    enPrimario: textoSobre(colorPrimario, colorTexto),
    enSecundario: textoSobre(colorSecundario, colorTexto),
    enAcento: textoSobre(colorAcento, colorTexto),
    primarioTexto: oscurecerParaTexto(colorPrimario, colorFondo),
    primarioSuave: mezclar(colorFondo, colorPrimario, 0.09),
    superficieAlterna,
    textoSuave: mezclaHastaCumplir(
      colorFondo,
      colorTexto,
      0.7,
      (c) =>
        contraste(c, colorFondo) >= 5.5 &&
        contraste(c, BLANCO) >= 5.5 &&
        contraste(c, superficieAlterna) >= 5.5,
    ),
    linea: mezclar(colorFondo, colorTexto, 0.1),
    bordeControl: mezclaHastaCumplir(
      colorFondo,
      colorTexto,
      0.55,
      (c) => contraste(c, colorFondo) >= 3 && contraste(c, BLANCO) >= 3,
    ),
  };
}

/** Derivados de un tema: los del diseño para la plantilla FitStore y las fórmulas para cualquier otra marca. */
export function derivarColores(tema: Tema): ColoresDerivados {
  const esFitstore = (Object.keys(TEMA_FITSTORE) as (keyof Tema)[]).every(
    (clave) => tema[clave].toUpperCase() === TEMA_FITSTORE[clave].toUpperCase(),
  );
  return esFitstore ? { ...COLORES_FITSTORE } : derivarPorFormula(tema);
}
