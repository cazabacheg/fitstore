import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentProviders, Provider, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp, type settingsApi } from 'shared';

/** Datos y ayudas de prueba de `features/configuracion` (no forman parte de la aplicación). */
export function configuracionAdmin(
  cambios: Partial<settingsApi.ConfiguracionAdmin> = {},
): settingsApi.ConfiguracionAdmin {
  return {
    identidad: {
      nombre: 'FitStore',
      eslogan: 'Ropa deportiva hecha en Gamarra.',
      razonSocial: 'Moda Activa S.A.C.',
      ruc: '20123456789',
      direccionFiscal: 'Jr. Gamarra 700, tienda 210, La Victoria, Lima',
      logoClaroUrl: null,
      logoOscuroUrl: null,
      faviconUrl: null,
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
      beneficios: [{ icono: '🚚', texto: 'Envíos a todo el Perú' }],
      email: 'ventas@fitstore.pe',
      telefono: '014567890',
      whatsapp: '987654321',
      instagramUrl: 'https://instagram.com/fitstore.pe',
      facebookUrl: null,
      tiktokUrl: null,
      horario: 'Lunes a sábado de 9:00 a 19:00',
      tiendaFisica: {
        direccion: 'Galería Gamarra, Jr. Gamarra 700, tienda 210',
        ubigeo: '150115',
        ubigeoNombre: 'La Victoria, Lima, Lima',
        referencia: null,
      },
      imagenesLanding: [
        {
          id: 11,
          tipo: 'LANDING',
          url: '/media/tienda/landing/1.jpg',
          orden: 0,
          textoAlternativo: 'Foto uno',
          enlace: null,
        },
        {
          id: 12,
          tipo: 'LANDING',
          url: '/media/tienda/landing/2.jpg',
          orden: 1,
          textoAlternativo: 'Foto dos',
          enlace: '/productos',
        },
      ],
    },
    parametros: {
      metodosPago: new Set(['CULQI', 'YAPE', 'MERCADO_PAGO']),
      modalidadesEntrega: new Set(['DELIVERY_LOCAL', 'AGENCIA', 'RECOJO']),
      expiracionPedidoMinutos: 30,
      diasReembolsoPostventa: 30,
      moneda: 'PEN',
    },
    politicas: { terminos: 1, privacidad: 1, cambiosDevoluciones: 1, envios: 1 },
    correosAlertas: ['ventas@fitstore.pe'],
    version: 4,
    actualizadoEn: '2026-10-19T15:00:00Z',
    actualizadoPor: 'admin',
    advertencias: [],
    ...cambios,
  };
}

export const respuesta = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

export const errorApi = (
  codigo: string,
  mensaje: string,
  errores?: { campo: string; mensaje: string }[],
) => ({
  success: false,
  mensaje,
  codigo,
  ...(errores ? { errores } : {}),
});

/** Monta un componente con HTTP simulado y devuelve ayudas para esperar a que se estabilice. */
export async function montarComponente<T>(
  tipo: Type<T>,
  entradas: Record<string, unknown> = {},
  proveedores: (Provider | EnvironmentProviders)[] = [],
) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      ...proveedores,
    ],
  });
  const fixture = TestBed.createComponent(tipo);
  for (const [nombre, valor] of Object.entries(entradas)) {
    fixture.componentRef.setInput(nombre, valor);
  }
  fixture.detectChanges();
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  const estabilizar = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };
  return { fixture, el, comp: fixture.componentInstance, estabilizar };
}

/** Cambia el valor de un campo del formulario y avisa como lo haría el navegador. */
export function escribir(campo: Element | null, valor: string): void {
  const entrada = campo as HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  entrada.value = valor;
  entrada.dispatchEvent(
    new Event(entrada instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }),
  );
}

export function porId<T extends Element = HTMLElement>(el: HTMLElement, id: string): T {
  const encontrado = el.querySelector<T>(`[data-testid="${id}"]`);
  if (!encontrado) throw new Error(`No existe [data-testid="${id}"]`);
  return encontrado;
}
