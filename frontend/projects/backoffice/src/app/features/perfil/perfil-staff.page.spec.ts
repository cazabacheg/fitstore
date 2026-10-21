import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ToastService, authApi } from 'shared';
import { montar } from '../../testing/montar';
import { SESION_OPERADOR } from '../../testing/sesiones';

const ME = '/api/v1/staff/me';
const CLAVE = '/api/v1/staff/me/password';
const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });
const http = () => TestBed.inject(HttpTestingController);
const PERFIL: authApi.Staff = {
  id: '0d2f8c1e-6a39-4d3b-8a53-1f6c2b9e7a10',
  email: 'operador@fitstore.pe',
  nombres: 'Omar',
  apellidos: 'Operador',
  rol: 'OPERADOR',
  estado: 'ACTIVO',
  ultimoLoginEn: null,
  creadoEn: '2026-10-01T10:00:00Z',
  version: 0,
};

async function abrir() {
  const h = await montar(SESION_OPERADOR, { url: '/perfil' });
  http().expectOne((r) => r.url === ME && r.method === 'GET').flush(ok(PERFIL));
  h.fixture.detectChanges();
  await h.fixture.whenStable();
  const el = () => h.routeNativeElement as HTMLElement;
  const estabilizar = async () => {
    h.fixture.detectChanges();
    await h.fixture.whenStable();
  };
  const campo = (etiqueta: string) =>
    [...el().querySelectorAll<HTMLElement>('.fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const entrada = (etiqueta: string) => campo(etiqueta).querySelector<HTMLInputElement>('input')!;
  const escribir = async (etiqueta: string, valor: string) => {
    entrada(etiqueta).value = valor;
    entrada(etiqueta).dispatchEvent(new Event('input'));
    await estabilizar();
  };
  const enviar = async (texto: string) => {
    [...el().querySelectorAll<HTMLButtonElement>('button[type="submit"]')]
      .find((b) => b.textContent?.includes(texto))!
      .click();
    await estabilizar();
  };
  const errorDe = (etiqueta: string) => campo(etiqueta).querySelector('.fs-campo__error')?.textContent ?? '';
  return { el, estabilizar, entrada, escribir, enviar, errorDe };
}

describe('Mi perfil del staff (P-1-10)', () => {
  it('CA-1-35 muestra mi nombre y, sin poder editarlos, mi correo y mi rol', async () => {
    const p = await abrir();

    expect(p.entrada('Nombres').value).toBe('Omar');
    expect(p.entrada('Apellidos').value).toBe('Operador');
    expect(p.entrada('Correo').value).toBe('operador@fitstore.pe');
    expect(p.entrada('Correo').disabled).toBe(true);
    expect(p.entrada('Rol').value).toBe('Operador');
    expect(p.entrada('Rol').disabled).toBe(true);
  });

  it('CA-1-35 al editar mi nombre se actualiza solo el nombre y avisa "Guardado"', async () => {
    const p = await abrir();
    await p.escribir('Nombres', 'Omar Luis');
    await p.enviar('Guardar cambios');

    const peticion = http().expectOne((r) => r.url === ME && r.method === 'PUT');
    expect(peticion.request.body).toEqual({ nombres: 'Omar Luis', apellidos: 'Operador' });
    peticion.flush(ok({ ...PERFIL, nombres: 'Omar Luis', version: 1 }));
    await p.estabilizar();

    expect(TestBed.inject(ToastService).toasts().map((t) => t.mensaje)).toContain('Guardado');
  });

  it('CA-1-35 un nombre vacío se indica y no se envía', async () => {
    const p = await abrir();
    await p.escribir('Nombres', '');
    await p.enviar('Guardar cambios');

    expect(p.errorDe('Nombres')).toContain('Ingresa tus nombres');
    http().expectNone((r) => r.url === ME && r.method === 'PUT');
  });

  it('CA-1-35 cambia mi contraseña con la actual y avisa', async () => {
    const p = await abrir();
    await p.escribir('Contraseña actual', 'Gamarra2026');
    await p.escribir('Nueva contraseña', 'NuevaClave2027');
    await p.escribir('Repite la nueva contraseña', 'NuevaClave2027');
    await p.enviar('Cambiar contraseña');

    const peticion = http().expectOne(CLAVE);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ passwordActual: 'Gamarra2026', nuevaPassword: 'NuevaClave2027' });
    peticion.flush(ok(null));
    await p.estabilizar();

    expect(TestBed.inject(ToastService).toasts().map((t) => t.mensaje)).toContain('Contraseña actualizada');
    expect(p.entrada('Contraseña actual').value).toBe('');
  });

  it('CA-1-35 una contraseña actual incorrecta se muestra en ese campo', async () => {
    const p = await abrir();
    await p.escribir('Contraseña actual', 'Incorrecta1');
    await p.escribir('Nueva contraseña', 'NuevaClave2027');
    await p.escribir('Repite la nueva contraseña', 'NuevaClave2027');
    await p.enviar('Cambiar contraseña');
    http()
      .expectOne(CLAVE)
      .flush(
        {
          success: false,
          mensaje: 'Revisa los datos ingresados',
          codigo: 'VALIDACION',
          errores: [{ campo: 'passwordActual', mensaje: 'La contraseña actual es incorrecta' }],
        },
        { status: 400, statusText: 'Bad Request' },
      );
    await p.estabilizar();

    expect(p.errorDe('Contraseña actual')).toContain('La contraseña actual es incorrecta');
  });

  it('CA-1-35 una nueva contraseña débil o que no coincide se indica antes de enviar', async () => {
    const p = await abrir();
    await p.escribir('Nueva contraseña', 'sinnumeros');
    await p.escribir('Repite la nueva contraseña', 'otra');
    await p.enviar('Cambiar contraseña');

    expect(p.errorDe('Contraseña actual')).toContain('Ingresa tu contraseña actual');
    expect(p.errorDe('Nueva contraseña')).toContain('al menos una letra y un número');
    expect(p.errorDe('Repite')).toContain('no coinciden');
    http().expectNone(CLAVE);
  });

  it('CA-1-35 si no se puede cargar el perfil muestra el error con "Reintentar"', async () => {
    const h = await montar(SESION_OPERADOR, { url: '/perfil' });
    http().expectOne((r) => r.url === ME).flush(null, { status: 500, statusText: 'Error' });
    h.fixture.detectChanges();
    await h.fixture.whenStable();
    const el = h.routeNativeElement as HTMLElement;

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Reintentar');
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Reintentar'))!.click();
    http().expectOne((r) => r.url === ME).flush(ok(PERFIL));
    h.fixture.detectChanges();
    await h.fixture.whenStable();
    expect(el.querySelector<HTMLInputElement>('input')?.value).toBe('Omar');
  });
});
