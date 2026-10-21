import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { SesionStore } from 'shared';
import { SESION_ADMIN } from '../../testing/sesiones';
import { montar } from '../../testing/montar';

const ok = <T>(data: T, mensaje = 'Operación exitosa') => ({ success: true, mensaje, data });
const http = () => TestBed.inject(HttpTestingController);

async function abrir(url: string) {
  const h = await montar(undefined, { url });
  const el = () => h.routeNativeElement as HTMLElement;
  const estabilizar = async () => {
    h.fixture.detectChanges();
    await h.fixture.whenStable();
  };
  const campo = (etiqueta: string) =>
    [...el().querySelectorAll<HTMLElement>('.fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const escribir = async (etiqueta: string, valor: string) => {
    const input = campo(etiqueta).querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await estabilizar();
  };
  const enviar = async () => {
    el().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await estabilizar();
  };
  const pedido = (url: string) => http().expectOne((r) => r.url === url);
  return { h, el, estabilizar, campo, escribir, enviar, pedido };
}

describe('Ingresar al backoffice (P-1-07)', () => {
  it('CA-1-22 un staff activo ingresa con su correo y contraseña, guarda la sesión y va al dashboard', async () => {
    const p = await abrir('/ingresar');
    expect(p.el().textContent).toContain('Ingresa al backoffice');
    await p.escribir('Correo', 'admin@fitstore.pe');
    await p.escribir('Contraseña', 'Gamarra2026');
    await p.enviar();

    const peticion = p.pedido('/api/v1/auth/staff/login');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ email: 'admin@fitstore.pe', password: 'Gamarra2026' });
    peticion.flush(ok(SESION_ADMIN));
    await p.estabilizar();

    expect(TestBed.inject(SesionStore).cuenta()?.rol).toBe('ADMIN');
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('CA-1-22 vuelve a la ruta de origen (?returnUrl=) y nunca a una dirección de otro sitio', async () => {
    const p = await abrir('/ingresar?returnUrl=%2Fclientes');
    await p.escribir('Correo', 'admin@fitstore.pe');
    await p.escribir('Contraseña', 'Gamarra2026');
    await p.enviar();
    p.pedido('/api/v1/auth/staff/login').flush(ok(SESION_ADMIN));
    await p.estabilizar();
    expect(TestBed.inject(Router).url).toBe('/clientes');

    TestBed.resetTestingModule();
    const q = await abrir('/ingresar?returnUrl=https%3A%2F%2Fmalo.com');
    await q.escribir('Correo', 'admin@fitstore.pe');
    await q.escribir('Contraseña', 'Gamarra2026');
    await q.enviar();
    q.pedido('/api/v1/auth/staff/login').flush(ok(SESION_ADMIN));
    await q.estabilizar();
    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('CA-1-23 un cliente o un invitado sin contraseña ve "Correo o contraseña incorrectos"', async () => {
    const p = await abrir('/ingresar');
    await p.escribir('Correo', 'cliente@correo.pe');
    await p.escribir('Contraseña', 'Gamarra2026');
    await p.enviar();
    p.pedido('/api/v1/auth/staff/login').flush(
      {
        success: false,
        mensaje: 'Correo o contraseña incorrectos',
        codigo: 'CREDENCIALES_INVALIDAS',
      },
      { status: 401, statusText: 'Unauthorized' },
    );
    await p.estabilizar();

    expect(p.el().querySelector('[role="alert"]')?.textContent).toContain(
      'Correo o contraseña incorrectos',
    );
    expect(TestBed.inject(SesionStore).autenticado()).toBe(false);
  });

  it('CA-1-24 con el bloqueo por intentos muestra "Cuenta bloqueada temporalmente"', async () => {
    const p = await abrir('/ingresar');
    await p.escribir('Correo', 'admin@fitstore.pe');
    await p.escribir('Contraseña', 'Gamarra2026');
    await p.enviar();
    p.pedido('/api/v1/auth/staff/login').flush(
      { success: false, mensaje: 'Cuenta bloqueada temporalmente', codigo: 'CUENTA_BLOQUEADA' },
      { status: 423, statusText: 'Locked' },
    );
    await p.estabilizar();

    expect(p.el().querySelector('[role="alert"]')?.textContent).toContain(
      'Cuenta bloqueada temporalmente',
    );
  });

  it('CA-1-22 un error del servidor muestra un mensaje genérico y un formulario vacío no envía nada', async () => {
    const p = await abrir('/ingresar');
    await p.enviar();
    expect(p.el().textContent).toContain('Ingresa tu correo');
    expect(p.el().textContent).toContain('Ingresa tu contraseña');
    http().expectNone('/api/v1/auth/staff/login');

    await p.escribir('Correo', 'admin@fitstore.pe');
    await p.escribir('Contraseña', 'Gamarra2026');
    await p.enviar();
    p.pedido('/api/v1/auth/staff/login').flush(null, { status: 500, statusText: 'Error' });
    await p.estabilizar();
    expect(p.el().querySelector('[role="alert"]')?.textContent).toContain('No pudimos ingresar');
  });

  it('CA-1-22 con una sesión de staff abierta, /ingresar lleva al dashboard', async () => {
    const h = await montar(SESION_ADMIN, { url: '/ingresar' });
    expect(TestBed.inject(Router).url).toBe('/');
    expect(h.routeNativeElement).not.toBeNull();
  });

  it('CA-1-22 el enlace "¿Olvidaste tu contraseña?" lleva a /recuperar', async () => {
    const p = await abrir('/ingresar');
    expect(p.el().querySelector('a[href="/recuperar"]')).not.toBeNull();
  });
});

describe('Recuperar contraseña del staff (P-1-03)', () => {
  it('CA-1-12 pide el enlace al endpoint de staff y muestra siempre el mismo mensaje', async () => {
    const p = await abrir('/recuperar');
    await p.escribir('Correo', 'admin@fitstore.pe');
    await p.enviar();

    const peticion = p.pedido('/api/v1/auth/staff/password/olvido');
    expect(peticion.request.body).toEqual({ email: 'admin@fitstore.pe' });
    peticion.flush(ok(null, 'Si el correo está registrado, te enviaremos un enlace'));
    await p.estabilizar();

    expect(p.el().textContent).toContain('Si el correo está registrado, te enviaremos un enlace');
  });

  it('CA-1-12 un correo inválido no envía nada', async () => {
    const p = await abrir('/recuperar');
    await p.escribir('Correo', 'sin-arroba');
    await p.enviar();

    expect(p.el().textContent).toContain('Ingresa un correo válido');
    http().expectNone('/api/v1/auth/staff/password/olvido');
  });
});

describe('Restablecer / crear contraseña del staff (P-1-04)', () => {
  const TOKEN = 't'.repeat(43);

  it('CA-1-13 con el enlace de recuperación el título es "Crea tu nueva contraseña"', async () => {
    const p = await abrir(`/restablecer?token=${TOKEN}`);
    expect(p.el().querySelector('h1')?.textContent).toContain('Crea tu nueva contraseña');
    expect(p.el().textContent).not.toContain('Bienvenido al equipo');
  });

  it('CA-1-26 con el enlace de invitación (invitacion=1) el título es "Bienvenido al equipo" y crea su contraseña', async () => {
    const p = await abrir(`/restablecer?token=${TOKEN}&invitacion=1`);
    expect(p.el().querySelector('h1')?.textContent).toContain('Bienvenido al equipo');
    await p.escribir('Nueva contraseña', 'Clave2026x');
    await p.escribir('Repite la contraseña', 'Clave2026x');
    await p.enviar();

    const peticion = p.pedido('/api/v1/auth/password/restablecer');
    expect(peticion.request.body).toEqual({ token: TOKEN, nuevaPassword: 'Clave2026x' });
    peticion.flush(ok(null, 'Contraseña actualizada'));
    await p.estabilizar();

    expect(p.el().textContent).toContain('Tu contraseña está lista');
    expect(p.el().querySelector('a[href="/ingresar"]')).not.toBeNull();
  });

  it('CA-1-13 una contraseña débil o que no coincide se indica antes de enviar', async () => {
    const p = await abrir(`/restablecer?token=${TOKEN}`);
    await p.escribir('Nueva contraseña', 'sinnumeros');
    await p.escribir('Repite la contraseña', 'otra');
    await p.enviar();

    expect(p.el().textContent).toContain('al menos una letra y un número');
    expect(p.el().textContent).toContain('Las contraseñas no coinciden');
    http().expectNone('/api/v1/auth/password/restablecer');
  });

  it('CA-1-14 un enlace vencido, usado o reemplazado muestra "Enlace inválido o expirado" con la opción de pedir otro', async () => {
    const p = await abrir(`/restablecer?token=${TOKEN}`);
    await p.escribir('Nueva contraseña', 'Clave2026x');
    await p.escribir('Repite la contraseña', 'Clave2026x');
    await p.enviar();
    p.pedido('/api/v1/auth/password/restablecer').flush(
      { success: false, mensaje: 'Enlace inválido o expirado', codigo: 'TOKEN_INVALIDO' },
      { status: 400, statusText: 'Bad Request' },
    );
    await p.estabilizar();

    expect(p.el().textContent).toContain('Enlace inválido o expirado');
    expect(p.el().querySelector('a[href="/recuperar"]')?.textContent).toContain(
      'Pedir otro enlace',
    );
  });

  it('CA-1-14 sin token en la dirección muestra directamente el enlace inválido', async () => {
    const p = await abrir('/restablecer');
    expect(p.el().textContent).toContain('Enlace inválido o expirado');
    expect(p.el().querySelector('button[type="submit"]')).toBeNull();
  });
});
