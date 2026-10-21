import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SesionStore, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { SESION_CLIENTE, montar } from '../../../testing';
import { FormularioIngreso } from './formulario-ingreso';

const RUTA = '/api/v1/auth/login';
const sesion = (data: unknown) => ({ success: true, mensaje: 'Sesión iniciada', data });

async function iniciar(returnUrl: string | null = '/carrito') {
  const m = await montar(FormularioIngreso, { returnUrl }, [
    provideRouter([]),
    provideFitstoreHttp({ carrito: false }),
    provideHttpClientTesting(),
    provideSesionConfig({ tipo: 'CLIENTE' }),
  ]);
  const ctrl = TestBed.inject(HttpTestingController);
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const escribir = async (etiqueta: string, valor: string) => {
    const input = m.el.querySelector<HTMLInputElement>(`input[type="${etiqueta}"]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await m.estabilizar();
  };
  const enviar = async () => {
    m.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await m.estabilizar();
  };
  const alerta = () => m.el.querySelector('[role="alert"]')?.textContent?.trim() ?? '';
  return { ...m, ctrl, navegar, escribir, enviar, alerta, store: TestBed.inject(SesionStore) };
}

describe('FormularioIngreso (P-1-01)', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('CA-1-04 ingresa con correo y contraseña, guarda la sesión y vuelve a la ruta de origen (?returnUrl=)', async () => {
    const f = await iniciar('/carrito');
    await f.escribir('email', 'rosa@correo.pe');
    await f.escribir('password', 'Gamarra2026');
    await f.enviar();

    const peticion = f.ctrl.expectOne(RUTA);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ email: 'rosa@correo.pe', password: 'Gamarra2026' });
    peticion.flush(sesion(SESION_CLIENTE));
    await f.estabilizar();

    expect(f.store.autenticado()).toBe(true);
    expect(f.store.cuenta()?.email).toBe('rosa@correo.pe');
    expect(f.navegar).toHaveBeenCalledWith('/carrito');
  });

  it('CA-1-04 sin returnUrl, o con uno que apunta fuera de la tienda, va al inicio', async () => {
    for (const origen of [null, 'https://malo.com/robar', '//malo.com', 'carrito']) {
      TestBed.resetTestingModule();
      const f = await iniciar(origen);
      await f.escribir('email', 'rosa@correo.pe');
      await f.escribir('password', 'Gamarra2026');
      await f.enviar();
      f.ctrl.expectOne(RUTA).flush(sesion(SESION_CLIENTE));
      await f.estabilizar();
      expect(f.navegar).toHaveBeenCalledWith('/');
    }
  });

  it('CA-1-04 mientras envía, el botón queda deshabilitado', async () => {
    const f = await iniciar();
    await f.escribir('email', 'rosa@correo.pe');
    await f.escribir('password', 'Gamarra2026');
    await f.enviar();

    expect(f.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    f.ctrl.expectOne(RUTA).flush(sesion(SESION_CLIENTE));
    await f.estabilizar();
  });

  it('CA-1-05 con credenciales incorrectas muestra "Correo o contraseña incorrectos" y no inicia sesión', async () => {
    const f = await iniciar();
    await f.escribir('email', 'rosa@correo.pe');
    await f.escribir('password', 'Incorrecta1');
    await f.enviar();
    f.ctrl
      .expectOne(RUTA)
      .flush(
        {
          success: false,
          mensaje: 'Correo o contraseña incorrectos',
          codigo: 'CREDENCIALES_INVALIDAS',
        },
        { status: 401, statusText: 'Unauthorized' },
      );
    await f.estabilizar();

    expect(f.alerta()).toContain('Correo o contraseña incorrectos');
    expect(f.store.autenticado()).toBe(false);
    expect(f.navegar).not.toHaveBeenCalled();
    expect(f.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('CA-1-06 con la cuenta bloqueada (423) muestra "Cuenta bloqueada temporalmente"', async () => {
    const f = await iniciar();
    await f.escribir('email', 'rosa@correo.pe');
    await f.escribir('password', 'Gamarra2026');
    await f.enviar();
    f.ctrl
      .expectOne(RUTA)
      .flush(
        { success: false, mensaje: 'Cuenta bloqueada temporalmente', codigo: 'CUENTA_BLOQUEADA' },
        { status: 423, statusText: 'Locked' },
      );
    await f.estabilizar();

    expect(f.alerta()).toContain('Cuenta bloqueada temporalmente');
    expect(f.alerta()).toContain('15 minutos');
  });

  it('CA-1-05 ante un error del servidor muestra un mensaje genérico', async () => {
    const f = await iniciar();
    await f.escribir('email', 'rosa@correo.pe');
    await f.escribir('password', 'Gamarra2026');
    await f.enviar();
    f.ctrl.expectOne(RUTA).flush(null, { status: 500, statusText: 'Server Error' });
    await f.estabilizar();

    expect(f.alerta()).toContain('No pudimos ingresar');
  });

  it('CA-1-04 con los campos vacíos o un correo inválido no envía nada y lo indica', async () => {
    const f = await iniciar();
    await f.enviar();
    expect(f.el.textContent).toContain('Ingresa tu correo');
    expect(f.el.textContent).toContain('Ingresa tu contraseña');

    await f.escribir('email', 'sin-arroba');
    await f.escribir('password', 'x');
    await f.enviar();
    expect(f.el.textContent).toContain('Ingresa un correo válido');
    f.ctrl.expectNone(RUTA);
  });
});
