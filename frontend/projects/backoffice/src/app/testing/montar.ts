import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig, type authApi } from 'shared';
import { crearRutas } from '../app.routes';

/** Simula `matchMedia` con un ancho de ventana (jsdom no lo trae). */
export function simularAncho(ancho: number): void {
  window.matchMedia = ((consulta: string) => {
    const minimo = /min-width:\s*(\d+)px/.exec(consulta);
    const maximo = /max-width:\s*(\d+)px/.exec(consulta);
    const coincide = (minimo ? ancho >= +minimo[1] : true) && (maximo ? ancho <= +maximo[1] : true);
    return {
      matches: coincide,
      media: consulta,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => false,
      onchange: null,
    };
  }) as typeof window.matchMedia;
}

/** Arma el backoffice real (rutas + shell) con HTTP simulado y una sesión opcional. */
export async function montar(
  sesion?: authApi.Sesion,
  opciones: { ancho?: number; proveedores?: Provider[]; url?: string } = {},
) {
  simularAncho(opciones.ancho ?? 1280);
  TestBed.configureTestingModule({
    providers: [
      provideRouter(crearRutas(true)),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
      ...(opciones.proveedores ?? []),
    ],
  });
  if (sesion) TestBed.inject(SesionStore).iniciarSesion(sesion);
  const harness = await RouterTestingHarness.create();
  if (opciones.url) await harness.navigateByUrl(opciones.url);
  return harness;
}
