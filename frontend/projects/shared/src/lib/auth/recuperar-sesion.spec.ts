import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationInitStatus, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ToastService } from '../http/toast.service';
import { provideFitstoreHttp } from '../http/proveedores';
import { SESION_STAFF } from '../testing/datos';
import { SesionStore } from './sesion.store';
import { provideSesion } from './proveedores';

function arrancar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesion({ tipo: 'STAFF' }),
    ],
  });
  const init = TestBed.inject(ApplicationInitStatus);
  return { init, ctrl: TestBed.inject(HttpTestingController) };
}

describe('Recuperación de la sesión al arrancar', () => {
  it('CA-0b-12 con una cookie válida, la app hace un refresh y recupera la sesión antes de arrancar', async () => {
    const { init, ctrl } = arrancar();
    expect(init.done).toBe(false);

    const refresh = ctrl.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.method).toBe('POST');
    refresh.flush({ success: true, mensaje: 'ok', data: SESION_STAFF });
    await init.donePromise;

    const store = TestBed.inject(SesionStore);
    expect(store.autenticado()).toBe(true);
    expect(store.token()).toBe(SESION_STAFF.accessToken);
  });

  it('CA-0b-12 sin cookie (401) la app arranca igual, sin sesión, sin toast y sin redirigir al login', async () => {
    const { init, ctrl } = arrancar();
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate');

    ctrl.expectOne('/api/v1/auth/refresh').flush(
      { success: false, mensaje: 'Tu sesión venció, vuelve a ingresar', codigo: 'NO_AUTENTICADO' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await init.donePromise;

    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
    expect(TestBed.inject(ToastService).toasts()).toHaveLength(0);
    expect(navegar).not.toHaveBeenCalled();
  });
});
