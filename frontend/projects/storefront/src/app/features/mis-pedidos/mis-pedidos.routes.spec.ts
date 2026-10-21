import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { SESION_CLIENTE } from '../../testing';
import { http } from '../cuenta/pruebas-cuenta';
import { MIS_PEDIDOS_ROUTES } from './mis-pedidos.routes';

async function abrir(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([{ path: 'cuenta/pedidos', children: MIS_PEDIDOS_ROUTES }]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'CLIENTE' }),
    ],
  });
  TestBed.inject(SesionStore).iniciarSesion(SESION_CLIENTE);
  const h = await RouterTestingHarness.create();
  await h.navigateByUrl(url);
  h.detectChanges();
  return h.routeNativeElement?.ownerDocument.body ?? document.body;
}

describe('Rutas de Mis pedidos', () => {
  afterEach(() => http().match(() => true));

  it('CA-4-33 la lista de pedidos se muestra dentro de "Mi cuenta", con su menú lateral', async () => {
    const cuerpo = await abrir('/cuenta/pedidos');
    const menu = cuerpo.querySelector('nav[aria-label="Mi cuenta"]');
    expect(menu).not.toBeNull();
    expect(menu?.querySelector('a.activo')?.textContent?.trim()).toBe('Mis pedidos');
    expect(cuerpo.querySelector('h1')?.textContent).toContain('Mis pedidos');
  });

  it('CA-4-34 el detalle de un pedido ocupa la página completa, sin el menú de la cuenta', async () => {
    const cuerpo = await abrir('/cuenta/pedidos/FS-000124');
    expect(cuerpo.querySelector('nav[aria-label="Mi cuenta"]')).toBeNull();
  });
});
