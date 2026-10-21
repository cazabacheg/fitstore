import { TestBed } from '@angular/core/testing';
import { ToastService } from 'shared';
import { montar } from '../../../testing';
import { PROVEEDORES, error400, http, ok } from '../pruebas-cuenta';
import { PerfilPage } from './perfil.page';

const ME = '/api/v1/clientes/me';
const PERFIL = {
  id: '5b1d7e22-9c40-4f1a-b7d2-3a8e6c0f4d55',
  email: 'rosa@correo.pe',
  nombres: 'Rosa',
  apellidos: 'Huamán Quispe',
  dni: '45678912',
  telefono: '987654321',
  creadoEn: '2026-10-01T10:00:00Z',
};

async function iniciar() {
  const m = await montar(PerfilPage, {}, PROVEEDORES);
  http().expectOne(ME).flush(ok(PERFIL));
  await m.estabilizar();
  const campo = (etiqueta: string) =>
    [...m.el.querySelectorAll<HTMLElement>('.fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const entrada = (etiqueta: string) => campo(etiqueta).querySelector<HTMLInputElement>('input')!;
  const escribir = async (etiqueta: string, valor: string) => {
    const input = entrada(etiqueta);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await m.estabilizar();
  };
  const guardar = async () => {
    [...m.el.querySelectorAll<HTMLButtonElement>('button[type="submit"]')]
      .find((b) => b.textContent?.includes('Guardar cambios'))!
      .click();
    await m.estabilizar();
  };
  const errorDe = (etiqueta: string) =>
    campo(etiqueta).querySelector('.fs-campo__error')?.textContent ?? '';
  return { ...m, campo, entrada, escribir, guardar, errorDe };
}

describe('PerfilPage (P-1-05)', () => {
  afterEach(() => http().verify());

  it('CA-1-15 muestra el perfil; el correo y el DNI se ven pero no se pueden editar', async () => {
    const p = await iniciar();

    expect(p.entrada('Nombres').value).toBe('Rosa');
    expect(p.entrada('Apellidos').value).toBe('Huamán Quispe');
    expect(p.entrada('Celular').value).toBe('987654321');
    expect(p.entrada('Correo').value).toBe('rosa@correo.pe');
    expect(p.entrada('Correo').disabled).toBe(true);
    expect(p.entrada('DNI').value).toBe('45678912');
    expect(p.entrada('DNI').disabled).toBe(true);
  });

  it('CA-1-15 al guardar nombres, apellidos y celular envía solo esos datos y avisa "Guardado"', async () => {
    const p = await iniciar();
    await p.escribir('Nombres', 'Rosa María');
    await p.escribir('Celular', '912345678');
    await p.guardar();

    const peticion = http().expectOne(ME);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      nombres: 'Rosa María',
      apellidos: 'Huamán Quispe',
      telefono: '912345678',
    });
    peticion.flush(ok({ ...PERFIL, nombres: 'Rosa María', telefono: '912345678' }));
    await p.estabilizar();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Guardado');
    expect(p.entrada('Nombres').value).toBe('Rosa María');
  });

  it('CA-1-15 un celular inválido o un nombre vacío se indican y no se envían', async () => {
    const p = await iniciar();
    await p.escribir('Nombres', '');
    await p.escribir('Celular', '812345678');
    await p.guardar();

    expect(p.errorDe('Nombres')).toContain('Ingresa tus nombres');
    expect(p.errorDe('Celular')).toContain('9 dígitos');
    http().expectNone(ME);
  });

  it('CA-1-15 los errores de campo que devuelve el backend se muestran junto al campo', async () => {
    const p = await iniciar();
    await p.guardar();
    http()
      .expectOne(ME)
      .flush(
        error400([
          { campo: 'telefono', mensaje: 'El celular debe tener 9 dígitos y empezar con 9' },
        ]),
        {
          status: 400,
          statusText: 'Bad Request',
        },
      );
    await p.estabilizar();

    expect(p.errorDe('Celular')).toContain('empezar con 9');
  });

  it('CA-1-15 si no se puede cargar el perfil muestra el error con "Reintentar"', async () => {
    const m = await montar(PerfilPage, {}, PROVEEDORES);
    http().expectOne(ME).flush(null, { status: 500, statusText: 'Error' });
    await m.estabilizar();

    expect(m.el.querySelector('[role="alert"]')?.textContent).toContain('Reintentar');
    [...m.el.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Reintentar'))!
      .click();
    http().expectOne(ME).flush(ok(PERFIL));
    await m.estabilizar();
    expect(m.el.querySelector<HTMLInputElement>('input:disabled')?.value).toBe('rosa@correo.pe');
  });
});
