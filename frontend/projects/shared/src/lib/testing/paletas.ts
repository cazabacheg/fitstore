/**
 * Las dos marcas del sistema de diseño (docs/diseno/sistema/tokens.json) con sus cinco colores configurables y los
 * derivados que el diseño fijó para cada una. Las pruebas del tema comparan contra estos valores.
 */
export interface Paleta {
  entrada: {
    colorPrimario: string;
    colorSecundario: string;
    colorAcento: string;
    colorFondo: string;
    colorTexto: string;
  };
  derivados: {
    enPrimario: string;
    enSecundario: string;
    enAcento: string;
    primarioTexto: string;
    primarioSuave: string;
    superficieAlterna: string;
    textoSuave: string;
    linea: string;
    bordeControl: string;
  };
}

export const PALETA_FITSTORE: Paleta = {
  entrada: {
    colorPrimario: '#C9B28C',
    colorSecundario: '#2B2621',
    colorAcento: '#4E5B45',
    colorFondo: '#FAF9F7',
    colorTexto: '#26221D',
  },
  derivados: {
    enPrimario: '#26221D',
    enSecundario: '#FFFFFF',
    enAcento: '#FFFFFF',
    primarioTexto: '#7A6343',
    primarioSuave: '#F3EDE3',
    superficieAlterna: '#F2F0EC',
    textoSuave: '#6A6259',
    linea: '#E6E1D9',
    bordeControl: '#8A8277',
  },
};

export const PALETA_KALLPA: Paleta = {
  entrada: {
    colorPrimario: '#B3245C',
    colorSecundario: '#22182E',
    colorAcento: '#A3E635',
    colorFondo: '#F8F6FB',
    colorTexto: '#22182E',
  },
  derivados: {
    enPrimario: '#FFFFFF',
    enSecundario: '#FFFFFF',
    enAcento: '#22182E',
    primarioTexto: '#B3245C',
    primarioSuave: '#FBE7EF',
    superficieAlterna: '#F1EDF6',
    textoSuave: '#625A6E',
    linea: '#E6E0EE',
    bordeControl: '#857B92',
  },
};
