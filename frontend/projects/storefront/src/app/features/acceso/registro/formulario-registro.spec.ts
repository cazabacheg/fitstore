import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SesionStore, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { SESION_CLIENTE, montar } from '../../../testing';
import { FormularioRegistro } from './formulario-registro';

const REGISTRO = '/api/v1/auth/registro';
const PUBLICA = '/api/v1/settings/public';
const POLITICAS = { terminos: 3, privacidad: 2, cambiosDevoluciones: 1, envios: 1 };
const ok = (data: unknown) => ({ success: true, mensaje: 'Operación exitosa', data });

async function iniciar(opciones: { politicas?: boolean } = {}) {
  // Sin carrito anónimo de otra prueba: si quedara un fs_carrito_id, el registro pediría la fusión del carrito
  localStorage.removeItem('fs_carrito_id');
  const m = await montar(FormularioRegistro, { returnUrl: '/checkout' }, [
    provideRouter([]),
    provideFitstoreHttp({ carrito: false }),
    provideHttpClientTesting(),
    provideSesionConfig({ tipo: 'CLIENTE' }),
  ]);
  const ctrl = TestBed.inject(HttpTestingController);
  const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  if (opciones.politicas !== false) {
    ctrl.expectOne(PUBLICA).flush(ok({ politicas: POLITICAS }));
    await m.estabilizar();
  }
  const campo = (etiqueta: string) =>
    [...m.el.querySelectorAll<HTMLElement>('.fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const escribir = async (etiqueta: string, valor: string) => {
    const input = campo(etiqueta).querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await m.estabilizar();
  };
  const marcar = async (indice: number) => {
    const casilla = m.el.querySelectorAll<HTMLInputElement>('input[type="checkbox"]')[indice];
    casilla.click();
    await m.estabilizar();
  };
  const llenarTodo = async () => {
    await escribir('Nombres', 'Ana');
    await escribir('Apellidos', 'Pérez Ríos');
    await escribir('DNI', '45678912');
    await escribir('Celular', '987654321');
    await escribir('Correo', 'ana@correo.com');
    await escribir('Contraseña', 'Gamarra2026');
    await marcar(0);
    await marcar(1);
  };
  const enviar = async () => {
    m.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await m.estabilizar();
  };
  const errorDe = (etiqueta: string) =>
    campo(etiqueta).querySelector('.fs-campo__error')?.textContent?.trim() ?? '';
  return {
    ...m,
    ctrl,
    navegar,
    escribir,
    marcar,
    llenarTodo,
    enviar,
    errorDe,
    store: TestBed.inject(SesionStore),
  };
}

describe('FormularioRegistro (P-1-02)', () => {
  afterEach(() => {
    // Tras registrarse se carga el carrito del cliente (contador de la bolsa): no es lo que estas pruebas miran
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl.match('/api/v1/carrito').forEach((r) => r.flush({ success: true, mensaje: 'ok', data: null }));
    ctrl.verify();
  });

  it('CA-1-01 envía el registro con las versiones de las políticas de /settings/public, inicia sesión y vuelve al origen', async () => {
    const f = await iniciar();
    await f.llenarTodo();
    await f.enviar();

    const peticion = f.ctrl.expectOne(REGISTRO);
    expect(peticion.request.body).toEqual({
      email: 'ana@correo.com',
      password: 'Gamarra2026',
      nombres: 'Ana',
      apellidos: 'Pérez Ríos',
      dni: '45678912',
      telefono: '987654321',
      aceptaTerminos: true,
      aceptaPrivacidad: true,
      versionTerminos: 3,
      versionPrivacidad: 2,
    });
    peticion.flush(ok(SESION_CLIENTE));
    await f.estabilizar();

    expect(f.store.autenticado()).toBe(true);
    expect(f.navegar).toHaveBeenCalledWith('/checkout');
  });

  it('CA-1-01 muestra las versiones vigentes junto a los enlaces de términos y privacidad (en otra pestaña)', async () => {
    const f = await iniciar();

    expect(f.el.textContent).toContain('(v3)');
    expect(f.el.textContent).toContain('(v2)');
    const enlaces = [...f.el.querySelectorAll<HTMLAnchorElement>('a[target="_blank"]')];
    expect(enlaces.map((a) => a.getAttribute('href'))).toEqual([
      '/politicas/terminos',
      '/politicas/privacidad',
    ]);
  });

  it('CA-1-01 si /settings/public falla muestra el error con "Reintentar" y no permite enviar', async () => {
    const f = await iniciar({ politicas: false });
    f.ctrl.expectOne(PUBLICA).flush(null, { status: 503, statusText: 'No disponible' });
    await f.estabilizar();

    expect(f.el.querySelector('[role="alert"]')?.textContent).toContain('Reintentar');
    expect(f.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    await f.llenarTodo();
    await f.enviar();
    f.ctrl.expectNone(REGISTRO);

    [...f.el.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Reintentar'))!
      .click();
    f.ctrl.expectOne(PUBLICA).flush(ok({ politicas: POLITICAS }));
    await f.estabilizar();
    expect(f.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('CA-1-02 un correo ya registrado se muestra junto al campo Correo', async () => {
    const f = await iniciar();
    await f.llenarTodo();
    await f.enviar();
    f.ctrl
      .expectOne(REGISTRO)
      .flush(
        { success: false, mensaje: 'El correo ya está registrado', codigo: 'CORREO_DUPLICADO' },
        { status: 409, statusText: 'Conflict' },
      );
    await f.estabilizar();

    expect(f.errorDe('Correo')).toContain('El correo ya está registrado');
    expect(f.errorDe('DNI')).toBe('');
    expect(f.store.autenticado()).toBe(false);
  });

  it('CA-1-02 un DNI ya registrado se muestra junto al campo DNI', async () => {
    const f = await iniciar();
    await f.llenarTodo();
    await f.enviar();
    f.ctrl
      .expectOne(REGISTRO)
      .flush(
        { success: false, mensaje: 'El DNI ya está registrado', codigo: 'DNI_DUPLICADO' },
        { status: 409, statusText: 'Conflict' },
      );
    await f.estabilizar();

    expect(f.errorDe('DNI')).toContain('El DNI ya está registrado');
  });

  it('CA-1-03 un DNI sin 8 dígitos, un celular mal formado, una contraseña débil o los consentimientos sin marcar se indican antes de enviar', async () => {
    const f = await iniciar();
    await f.escribir('Nombres', 'Ana');
    await f.escribir('Apellidos', 'Pérez');
    await f.escribir('DNI', '1234567');
    await f.escribir('Celular', '812345678');
    await f.escribir('Correo', 'ana@correo.com');
    await f.escribir('Contraseña', 'sinnumeros');
    await f.enviar();

    expect(f.errorDe('DNI')).toContain('8 dígitos');
    expect(f.errorDe('Celular')).toContain('9 dígitos');
    expect(f.errorDe('Contraseña')).toContain('al menos una letra y un número');
    expect(f.el.textContent).toContain('Debes aceptar los términos y condiciones');
    expect(f.el.textContent).toContain('Debes aceptar la política de privacidad');
    f.ctrl.expectNone(REGISTRO);
  });

  it('CA-1-03 el celular debe tener 9 dígitos y empezar con 9', async () => {
    const f = await iniciar();
    for (const malo of ['98765432', '9876543210', '812345678', '98765432a']) {
      await f.escribir('Celular', malo);
      await f.enviar();
      expect(f.errorDe('Celular')).toContain('9 dígitos');
    }
    await f.escribir('Celular', '987654321');
    await f.enviar();
    expect(f.errorDe('Celular')).toBe('');
  });

  it('CA-1-03 si el backend responde 400 con campos igual los muestra junto a cada campo', async () => {
    const f = await iniciar();
    await f.llenarTodo();
    await f.enviar();
    f.ctrl.expectOne(REGISTRO).flush(
      {
        success: false,
        mensaje: 'Revisa los datos ingresados',
        codigo: 'VALIDACION',
        errores: [
          { campo: 'dni', mensaje: 'El DNI debe tener 8 dígitos' },
          { campo: 'telefono', mensaje: 'El celular debe tener 9 dígitos y empezar con 9' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await f.estabilizar();

    expect(f.errorDe('DNI')).toContain('El DNI debe tener 8 dígitos');
    expect(f.errorDe('Celular')).toContain('empezar con 9');
  });

  it('CA-1-03 el indicador de fortaleza sube con una contraseña más segura', async () => {
    const f = await iniciar();
    const nivel = () => f.el.querySelector('[data-fortaleza]')?.getAttribute('data-fortaleza');
    await f.escribir('Contraseña', 'abc');
    const debil = Number(nivel());
    await f.escribir('Contraseña', 'Gamarra2026!Fuerte');
    expect(Number(nivel())).toBeGreaterThan(debil);
  });
});
