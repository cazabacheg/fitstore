import type { catalogApi, settingsApi } from 'shared';

/** Datos de prueba de la landing (solo para las pruebas de `features/inicio`). */
export function configuracionPublica(
  cambios: Partial<settingsApi.Contenido> = {},
): settingsApi.ConfiguracionPublica {
  return {
    identidad: {
      nombre: 'FitStore',
      razonSocial: 'Moda Activa S.A.C.',
      ruc: '20123456789',
      direccionFiscal: 'Jr. Gamarra 700, La Victoria, Lima',
    },
    tema: {
      colorPrimario: '#C9B28C',
      colorSecundario: '#2B2621',
      colorAcento: '#4E5B45',
      colorFondo: '#FAF9F7',
      colorTexto: '#26221D',
    },
    contenido: {
      heroTitulo: 'Muévete con estilo',
      heroSubtitulo: 'Leggings, tops y casacas hechos en Gamarra.',
      heroCtaTexto: 'Ver novedades',
      heroCtaEnlace: '/productos',
      beneficios: [
        { icono: '🚚', texto: 'Envíos a todo el Perú' },
        { icono: '↩', texto: 'Cambios hasta 30 días' },
        { icono: '🔒', texto: 'Pago seguro' },
      ],
      email: 'ventas@fitstore.pe',
      whatsapp: '987654321',
      horario: 'Lunes a sábado de 9:00 a 19:00',
      tiendaFisica: {
        direccion: 'Galería Gamarra, Jr. Gamarra 700, tienda 210',
        ubigeo: '150115',
        ubigeoNombre: 'La Victoria, Lima, Lima',
      },
      imagenesLanding: [
        { id: 1, tipo: 'LANDING', url: '/media/tienda/landing/1.jpg', orden: 0, textoAlternativo: 'Foto uno', enlace: '/productos' },
        { id: 2, tipo: 'LANDING', url: '/media/tienda/landing/2.jpg', orden: 1, textoAlternativo: 'Foto dos', enlace: null },
        { id: 3, tipo: 'LANDING', url: '/media/tienda/landing/3.jpg', orden: 2, textoAlternativo: 'Foto tres', enlace: 'https://tienda.pe/oferta' },
      ],
      ...cambios,
    },
    parametros: {
      metodosPago: new Set(['CULQI', 'YAPE']),
      modalidadesEntrega: new Set(['DELIVERY_LOCAL']),
      expiracionPedidoMinutos: 30,
      diasReembolsoPostventa: 30,
      moneda: 'PEN',
    },
    politicas: { terminos: 1, privacidad: 1, cambiosDevoluciones: 1, envios: 1 },
  };
}

export const CATEGORIAS: catalogApi.CategoriaNodo[] = [
  { id: 101, nombre: 'Polos y tops', slug: 'polos-y-tops', hijas: [] },
  { id: 102, nombre: 'Leggings y pantalones', slug: 'leggings-y-pantalones', hijas: [] },
  { id: 103, nombre: 'Casacas y polerones', slug: 'casacas-y-polerones', hijas: [] },
];

export function destacados(cantidad: number): catalogApi.ProductoTarjeta[] {
  return Array.from({ length: cantidad }, (_, i) => ({
    id: 1001 + i,
    slug: `producto-${i + 1}`,
    nombre: `Producto ${i + 1}`,
    imagenUrl: null,
    precioDesde: 59.9,
    colores: [],
    agotado: false,
  }));
}

export function respuesta<T>(data: T) {
  return { success: true, mensaje: 'Operación exitosa', data };
}
