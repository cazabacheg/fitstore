import { PALETA_FITSTORE, PALETA_KALLPA } from '../testing/paletas';
import {
  contraste,
  derivarColores,
  derivarPorFormula,
  hexARgb,
  mezclar,
  type Tema,
} from './colores';

/** Diferencia máxima por canal (0-255) entre dos colores. */
function distancia(a: string, b: string): number {
  const [x, y] = [hexARgb(a), hexARgb(b)];
  return Math.max(...x.map((v, i) => Math.abs(v - y[i])));
}

const BLANCO = '#FFFFFF';

describe('contraste y mezcla', () => {
  it('CA-0b-05 el contraste WCAG de blanco sobre negro es 21 y el de un color consigo mismo es 1', () => {
    expect(contraste('#FFFFFF', '#000000')).toBeCloseTo(21, 1);
    expect(contraste('#C9B28C', '#C9B28C')).toBeCloseTo(1, 5);
  });

  it('CA-0b-05 mezclar devuelve los extremos con t = 0 y t = 1 y el punto medio en 0.5', () => {
    expect(mezclar('#000000', '#FFFFFF', 0)).toBe('#000000');
    expect(mezclar('#000000', '#FFFFFF', 1)).toBe('#FFFFFF');
    expect(mezclar('#000000', '#FFFFFF', 0.5)).toBe('#808080');
  });
});

describe('derivarColores según el sistema de diseño', () => {
  const casos: [string, typeof PALETA_FITSTORE][] = [
    ['FitStore', PALETA_FITSTORE],
    ['Kallpa', PALETA_KALLPA],
  ];

  it.each(casos)(
    'CA-0b-05 %s: en-primario, en-secundario y en-acento eligen blanco o el texto, como el diseño',
    (_n, paleta) => {
      const d = derivarPorFormula(paleta.entrada);
      expect(d.enPrimario).toBe(paleta.derivados.enPrimario);
      expect(d.enSecundario).toBe(paleta.derivados.enSecundario);
      expect(d.enAcento).toBe(paleta.derivados.enAcento);
    },
  );

  it('CA-0b-05 Kallpa: su primario ya se lee como texto, así que primario-texto es el mismo primario', () => {
    expect(derivarPorFormula(PALETA_KALLPA.entrada).primarioTexto).toBe('#B3245C');
  });

  it.each(casos)(
    'CA-0b-05 %s: los demás derivados quedan a ≤ 10 por canal de los valores del diseño',
    (_n, paleta) => {
      const d = derivarPorFormula(paleta.entrada);
      for (const clave of [
        'primarioSuave',
        'superficieAlterna',
        'textoSuave',
        'linea',
        'bordeControl',
      ] as const) {
        expect(distancia(d[clave], paleta.derivados[clave]), clave).toBeLessThanOrEqual(10);
      }
    },
  );

  it('CA-0b-05 con el tema FitStore por defecto los derivados son exactamente los del diseño', () => {
    expect(derivarColores(PALETA_FITSTORE.entrada)).toEqual(PALETA_FITSTORE.derivados);
    expect(derivarColores({ ...PALETA_FITSTORE.entrada, colorPrimario: '#c9b28c' })).toEqual(
      PALETA_FITSTORE.derivados,
    );
  });

  const temas: [string, Tema][] = [
    ['FitStore', PALETA_FITSTORE.entrada],
    ['Kallpa', PALETA_KALLPA.entrada],
    [
      'rojo #E11D48 (ejemplo de CA-0b-05)',
      {
        colorPrimario: '#E11D48',
        colorSecundario: '#111827',
        colorAcento: '#F59E0B',
        colorFondo: '#FFFFFF',
        colorTexto: '#111827',
      },
    ],
    [
      'primario muy claro',
      {
        colorPrimario: '#F5E6A8',
        colorSecundario: '#1F2937',
        colorAcento: '#FDE68A',
        colorFondo: '#FFFDF5',
        colorTexto: '#1C1917',
      },
    ],
    [
      'primario muy oscuro',
      {
        colorPrimario: '#0F172A',
        colorSecundario: '#0F172A',
        colorAcento: '#0EA5E9',
        colorFondo: '#F8FAFC',
        colorTexto: '#0F172A',
      },
    ],
    [
      'fondo gris',
      {
        colorPrimario: '#16A34A',
        colorSecundario: '#14532D',
        colorAcento: '#FACC15',
        colorFondo: '#E5E7EB',
        colorTexto: '#111827',
      },
    ],
  ];

  it.each(temas)(
    'CA-0b-05 %s: los derivados cumplen los contrastes del sistema de diseño',
    (_n, tema) => {
      const d = derivarPorFormula(tema);
      expect(contraste(d.enPrimario, tema.colorPrimario)).toBeGreaterThanOrEqual(4.5);
      expect(contraste(d.enSecundario, tema.colorSecundario)).toBeGreaterThanOrEqual(4.5);
      expect(contraste(d.enAcento, tema.colorAcento)).toBeGreaterThanOrEqual(4.5);
      expect(contraste(d.primarioTexto, tema.colorFondo)).toBeGreaterThanOrEqual(4.5);
      expect(contraste(d.textoSuave, tema.colorFondo)).toBeGreaterThanOrEqual(5.5);
      expect(contraste(d.textoSuave, BLANCO)).toBeGreaterThanOrEqual(5.5);
      expect(contraste(d.textoSuave, d.superficieAlterna)).toBeGreaterThanOrEqual(5.5);
      expect(contraste(d.bordeControl, tema.colorFondo)).toBeGreaterThanOrEqual(3);
      expect(contraste(d.bordeControl, BLANCO)).toBeGreaterThanOrEqual(3);
    },
  );

  it('CA-0b-05 un primario claro se oscurece como texto pero conserva su tono', () => {
    const d = derivarPorFormula(PALETA_FITSTORE.entrada);
    const [r, g, b] = hexARgb(d.primarioTexto);
    expect(r).toBeGreaterThan(b); // sigue siendo cálido (arena), no gris
    expect(contraste(d.primarioTexto, '#FAF9F7')).toBeLessThan(7); // no se oscurece de más
  });
});
