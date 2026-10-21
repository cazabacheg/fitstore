import { TestBed } from '@angular/core/testing';
import { ToastService } from 'shared';
import { montar } from '../../../testing';
import { PROVEEDORES, error400, http, ok } from '../pruebas-cuenta';
import { CambioPassword } from './cambio-password';

const RUTA = '/api/v1/clientes/me/password';

async function iniciar() {
  const m = await montar(CambioPassword, {}, PROVEEDORES);
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
  const llenar = async (actual: string, nueva: string, repetida = nueva) => {
    await escribir('Contraseña actual', actual);
    await escribir('Nueva contraseña', nueva);
    await escribir('Repite', repetida);
    m.el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await m.estabilizar();
  };
  const errorDe = (etiqueta: string) =>
    campo(etiqueta).querySelector('.fs-campo__error')?.textContent ?? '';
  return { ...m, llenar, errorDe, campo };
}

describe('CambioPassword (P-1-05)', () => {
  afterEach(() => http().verify());

  it('CA-1-16 con la contraseña actual correcta y una nueva válida la cambia y avisa', async () => {
    const c = await iniciar();
    await c.llenar('Gamarra2026', 'NuevaClave2027');

    const peticion = http().expectOne(RUTA);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      passwordActual: 'Gamarra2026',
      nuevaPassword: 'NuevaClave2027',
    });
    peticion.flush(ok(null, 'Contraseña actualizada'));
    await c.estabilizar();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Contraseña actualizada');
    expect(c.campo('Contraseña actual').querySelector<HTMLInputElement>('input')!.value).toBe('');
  });

  it('CA-1-16 si la contraseña actual es incorrecta, el error aparece en ese campo', async () => {
    const c = await iniciar();
    await c.llenar('Incorrecta1', 'NuevaClave2027');
    http()
      .expectOne(RUTA)
      .flush(
        error400([{ campo: 'passwordActual', mensaje: 'La contraseña actual es incorrecta' }]),
        {
          status: 400,
          statusText: 'Bad Request',
        },
      );
    await c.estabilizar();

    expect(c.errorDe('Contraseña actual')).toContain('La contraseña actual es incorrecta');
    expect(c.errorDe('Nueva contraseña')).toBe('');
  });

  it('CA-1-16 una nueva contraseña débil, o que no coincide, se indica antes de enviar', async () => {
    const c = await iniciar();
    await c.llenar('', 'sinnumeros', 'otra');

    expect(c.errorDe('Contraseña actual')).toContain('Ingresa tu contraseña actual');
    expect(c.errorDe('Nueva contraseña')).toContain('al menos una letra y un número');
    expect(c.errorDe('Repite')).toContain('no coinciden');
    http().expectNone(RUTA);
  });
});
