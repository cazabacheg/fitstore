import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Provider, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Routes, provideRouter } from '@angular/router';
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
  opciones: { ancho?: number; proveedores?: Provider[]; url?: string; rutas?: Routes } = {},
) {
  simularAncho(opciones.ancho ?? 1280);
  TestBed.configureTestingModule({
    providers: [
      provideRouter(opciones.rutas ?? crearRutas(true)),
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

/** Pantalla neutra para las pruebas del layout: no pide datos ni depende de la spec dueña de la carpeta. */
@Component({
  selector: 'fs-pantalla-de-prueba',
  template: '<h1>{{ titulo }}</h1><p>Pantalla de prueba</p>',
})
class PantallaDePrueba {
  /** Como las pantallas reales, muestra el título de su ruta. */
  protected readonly titulo = (inject(ActivatedRoute).snapshot.data['titulo'] as string) ?? '';
}

function aPrueba(rutas: Routes): Routes {
  return rutas.map((ruta) => ({
    ...ruta,
    ...(ruta.component ? { component: PantallaDePrueba } : {}),
    ...(ruta.loadComponent ? { loadComponent: () => PantallaDePrueba } : {}),
    ...(ruta.children ? { children: aPrueba(ruta.children) } : {}),
  }));
}

/**
 * Rutas de la app con cada pantalla de `features/` cambiada por `PantallaDePrueba`, conservando rutas, guardas y
 * datos. Así las pruebas del layout siguen pasando cuando una spec reemplaza su marcador por una pantalla real que
 * hace sus propias peticiones.
 */
export function conPantallasDePrueba(rutas: Routes): Routes {
  return rutas.map((ruta) => ({
    ...ruta,
    ...(ruta.loadChildren
      ? {
          loadChildren: async () => {
            const cargado = await (ruta.loadChildren as () => Promise<unknown>)();
            return aPrueba(cargado as Routes);
          },
        }
      : {}),
    ...(ruta.children ? { children: conPantallasDePrueba(ruta.children) } : {}),
  }));
}

/** `montar` para las pruebas del layout: las pantallas de `features/` se cambian por `PantallaDePrueba`. */
export function montarShell(
  sesion?: authApi.Sesion,
  opciones: { ancho?: number; proveedores?: Provider[]; url?: string } = {},
) {
  return montar(sesion, { ...opciones, rutas: conPantallasDePrueba(crearRutas(true)) });
}
