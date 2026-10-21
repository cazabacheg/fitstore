import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideFitstoreHttp, provideSesionConfig } from 'shared';
import { montar } from '../../../testing';
import { RecuperarPage } from './recuperar.page';
import { RestablecerPage } from '../restablecer/restablecer.page';

const ok = (mensaje: string) => ({ success: true, mensaje, data: null });

function proveedores(token?: string) {
  return [
    provideRouter([]),
    provideFitstoreHttp({ carrito: false }),
    provideHttpClientTesting(),
    provideSesionConfig({ tipo: 'CLIENTE' }),
    {
      provide: ActivatedRoute,
      useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } },
    },
  ];
}

function utilidades(m: Awaited<ReturnType<typeof montar>>) {
  const escribir = async (selector: string, valor: string) => {
    const input = m.el.querySelectorAll<HTMLInputElement>(selector)[0];
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await m.estabilizar();
  };
  const campo = async (etiqueta: string, valor: string) => {
    const input = [...m.el.querySelectorAll<HTMLElement>('.fs-campo')]
      .find((c) => c.querySelector('label')?.textContent?.trim().startsWith(etiqueta))!
      .querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await m.estabilizar();
  };
  const enviar = async () => {
    m.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await m.estabilizar();
  };
  return { escribir, campo, enviar };
}

describe('RecuperarPage (P-1-03)', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('CA-1-12 pide el enlace con el correo y muestra siempre "Si el correo está registrado, te enviaremos un enlace"', async () => {
    const m = await montar(RecuperarPage, {}, proveedores());
    const u = utilidades(m);
    await u.campo('Correo', 'ana@correo.com');
    await u.enviar();

    const peticion = TestBed.inject(HttpTestingController).expectOne(
      '/api/v1/auth/password/olvido',
    );
    expect(peticion.request.body).toEqual({ email: 'ana@correo.com' });
    peticion.flush(ok('Si el correo está registrado, te enviaremos un enlace'));
    await m.estabilizar();

    expect(m.el.textContent).toContain('Si el correo está registrado, te enviaremos un enlace');
  });

  it('CA-1-12 un correo con formato inválido no envía nada', async () => {
    const m = await montar(RecuperarPage, {}, proveedores());
    const u = utilidades(m);
    await u.campo('Correo', 'sin-arroba');
    await u.enviar();

    TestBed.inject(HttpTestingController).expectNone('/api/v1/auth/password/olvido');
    expect(m.el.textContent).toContain('Ingresa un correo válido');
  });
});

describe('RestablecerPage (P-1-04)', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('CA-1-13 crea la nueva contraseña con el token del enlace y ofrece ingresar', async () => {
    const m = await montar(RestablecerPage, {}, proveedores('t'.repeat(43)));
    const u = utilidades(m);
    await u.campo('Nueva contraseña', 'NuevaClave2027');
    await u.campo('Repite la contraseña', 'NuevaClave2027');
    await u.enviar();

    const peticion = TestBed.inject(HttpTestingController).expectOne(
      '/api/v1/auth/password/restablecer',
    );
    expect(peticion.request.body).toEqual({
      token: 't'.repeat(43),
      nuevaPassword: 'NuevaClave2027',
    });
    peticion.flush(ok('Contraseña actualizada'));
    await m.estabilizar();

    expect(m.el.textContent).toContain('Tu contraseña se actualizó');
    expect(m.el.querySelector('a[href="/ingresar"]')).not.toBeNull();
  });

  it('CA-1-13 una contraseña que no cumple la política o que no coincide se indica antes de enviar', async () => {
    const m = await montar(RestablecerPage, {}, proveedores('t'.repeat(43)));
    const u = utilidades(m);
    await u.campo('Nueva contraseña', 'sinnumeros');
    await u.campo('Repite la contraseña', 'otra');
    await u.enviar();

    expect(m.el.textContent).toContain('al menos una letra y un número');
    expect(m.el.textContent).toContain('Las contraseñas no coinciden');
    TestBed.inject(HttpTestingController).expectNone('/api/v1/auth/password/restablecer');
  });

  it('CA-1-14 un enlace vencido, usado o reemplazado muestra "Enlace inválido o expirado" con la opción de pedir otro', async () => {
    const m = await montar(RestablecerPage, {}, proveedores('t'.repeat(43)));
    const u = utilidades(m);
    await u.campo('Nueva contraseña', 'NuevaClave2027');
    await u.campo('Repite la contraseña', 'NuevaClave2027');
    await u.enviar();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/auth/password/restablecer')
      .flush(
        { success: false, mensaje: 'Enlace inválido o expirado', codigo: 'TOKEN_INVALIDO' },
        { status: 400, statusText: 'Bad Request' },
      );
    await m.estabilizar();

    expect(m.el.textContent).toContain('Enlace inválido o expirado');
    expect(m.el.querySelector('a[href="/recuperar"]')?.textContent).toContain('Pedir otro enlace');
  });

  it('CA-1-14 sin token en la dirección muestra directamente el enlace inválido', async () => {
    const m = await montar(RestablecerPage, {}, proveedores());

    expect(m.el.textContent).toContain('Enlace inválido o expirado');
    expect(m.el.querySelector('a[href="/recuperar"]')).not.toBeNull();
    expect(m.el.querySelector('button[type="submit"]')).toBeNull();
  });
});
