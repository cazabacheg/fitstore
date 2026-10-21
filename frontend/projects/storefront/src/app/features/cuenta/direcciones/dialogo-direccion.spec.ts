import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { authApi } from 'shared';
import { FsSelectorUbigeo } from '../../../ui/selector-ubigeo/selector-ubigeo';
import { PROVEEDORES, error400, http, ok } from '../pruebas-cuenta';
import { DialogoDireccionService } from './dialogo-direccion.service';

const RUTA = '/api/v1/clientes/me/direcciones';
const GUARDADA: authApi.Direccion = {
  id: 7,
  alias: 'Casa',
  ubigeo: '150115',
  direccion: 'Jr. Gamarra 123, dpto 4',
  referencia: 'Frente al parque',
  telefonoContacto: null,
  predeterminada: true,
};

async function abrir(direccion?: authApi.Direccion) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...PROVEEDORES] });
  const servicio = TestBed.inject(DialogoDireccionService);
  const resultado = servicio.abrir(direccion);
  const refrescar = async () => {
    TestBed.inject(ApplicationRef).tick();
    await new Promise((r) => setTimeout(r, 0));
    TestBed.inject(ApplicationRef).tick();
  };
  await refrescar();
  // el selector de ubigeo pide los departamentos al montarse
  http()
    .match((r) => r.url.includes('/ubigeo/'))
    .forEach((r) => r.flush(ok([])));
  const dialogo = () => document.body.querySelector<HTMLElement>('[role="dialog"]');
  const campo = (etiqueta: string) =>
    [...document.body.querySelectorAll<HTMLElement>('[role="dialog"] .fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const escribir = async (etiqueta: string, valor: string) => {
    const input = campo(etiqueta).querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await refrescar();
  };
  const elegirUbigeo = async (codigo: string | null) => {
    getSelector().cambio.emit(codigo);
    await refrescar();
  };
  const boton = (texto: string) =>
    [...document.body.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  const errorDe = (etiqueta: string) =>
    campo(etiqueta).querySelector('.fs-campo__error')?.textContent ?? '';
  return { resultado, refrescar, dialogo, campo, escribir, elegirUbigeo, boton, errorDe };
}

/** Instancia del selector de ubigeo que está dentro del diálogo abierto. */
function getSelector(): FsSelectorUbigeo {
  const el = document.body.querySelector('fs-selector-ubigeo')!;
  const depurar = (window as unknown as { ng: { getComponent(e: Element): FsSelectorUbigeo } }).ng;
  return depurar.getComponent(el);
}

describe('DialogoDireccion y su servicio (A6)', () => {
  afterEach(() => {
    http().verify();
    document.body.querySelectorAll('fs-dialogo-direccion').forEach((e) => e.remove());
  });

  it('CA-1-17 abrir() sin dirección crea una nueva y devuelve la dirección guardada', async () => {
    const d = await abrir();
    expect(d.dialogo()).not.toBeNull();
    expect(d.dialogo()?.textContent).toContain('Agregar dirección');
    await d.escribir('Alias', 'Casa');
    await d.escribir('Dirección', 'Jr. Gamarra 123, dpto 4');
    await d.escribir('Referencia', 'Frente al parque');
    await d.elegirUbigeo('150115');
    d.boton('Guardar').click();
    await d.refrescar();

    const peticion = http().expectOne(RUTA);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      alias: 'Casa',
      ubigeo: '150115',
      direccion: 'Jr. Gamarra 123, dpto 4',
      referencia: 'Frente al parque',
      telefonoContacto: null,
    });
    peticion.flush(ok(GUARDADA));
    await d.refrescar();

    expect(await d.resultado).toEqual(GUARDADA);
    expect(d.dialogo()).toBeNull();
  });

  it('CA-1-19 abrir(direccion) edita: trae los datos y guarda con PUT sobre su id', async () => {
    const d = await abrir(GUARDADA);
    expect(d.dialogo()?.textContent).toContain('Editar dirección');
    expect(d.campo('Alias').querySelector<HTMLInputElement>('input')!.value).toBe('Casa');
    await d.escribir('Alias', 'Casa de campo');
    d.boton('Guardar').click();
    await d.refrescar();

    const peticion = http().expectOne(`${RUTA}/7`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.alias).toBe('Casa de campo');
    expect(peticion.request.body.ubigeo).toBe('150115');
    peticion.flush(ok({ ...GUARDADA, alias: 'Casa de campo' }));
    await d.refrescar();

    expect((await d.resultado)?.alias).toBe('Casa de campo');
  });

  it('CA-1-17 si se cancela devuelve nada y no llama a la API', async () => {
    const d = await abrir();
    d.boton('Cancelar').click();
    await d.refrescar();

    expect(await d.resultado).toBeUndefined();
    expect(d.dialogo()).toBeNull();
    http().expectNone(RUTA);
  });

  it('CA-1-17 pide alias, distrito y una dirección de al menos 5 caracteres, y valida el celular de contacto', async () => {
    const d = await abrir();
    await d.escribir('Dirección', 'abc');
    await d.escribir('Celular de contacto', '812345678');
    d.boton('Guardar').click();
    await d.refrescar();

    expect(d.errorDe('Alias')).toContain('Ponle un nombre');
    expect(d.errorDe('Dirección')).toContain('al menos 5');
    expect(d.errorDe('Celular de contacto')).toContain('9 dígitos');
    expect(d.dialogo()?.textContent).toContain('Elige el distrito');
    http().expectNone(RUTA);
    d.boton('Cancelar').click();
    await d.refrescar();
  });

  it('CA-1-20 con 10 direcciones el backend responde 409 y el diálogo lo explica sin cerrarse', async () => {
    const d = await abrir();
    await d.escribir('Alias', 'Once');
    await d.escribir('Dirección', 'Jr. Gamarra 123, dpto 4');
    await d.elegirUbigeo('150115');
    d.boton('Guardar').click();
    await d.refrescar();
    http().expectOne(RUTA).flush(
      {
        success: false,
        mensaje: 'Ya tienes 10 direcciones guardadas',
        codigo: 'LIMITE_DIRECCIONES',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await d.refrescar();

    expect(d.dialogo()?.textContent).toContain('Llegaste al máximo de 10 direcciones');
    d.boton('Cancelar').click();
    await d.refrescar();
    expect(await d.resultado).toBeUndefined();
  });

  it('CA-1-17 los errores de campo del backend se muestran junto al campo', async () => {
    const d = await abrir();
    await d.escribir('Alias', 'Casa');
    await d.escribir('Dirección', 'Jr. Gamarra 123, dpto 4');
    await d.elegirUbigeo('150115');
    d.boton('Guardar').click();
    await d.refrescar();
    http()
      .expectOne(RUTA)
      .flush(error400([{ campo: 'direccion', mensaje: 'Debe tener entre 5 y 200 caracteres' }]), {
        status: 400,
        statusText: 'Bad Request',
      });
    await d.refrescar();

    expect(d.errorDe('Dirección')).toContain('entre 5 y 200');
    d.boton('Cancelar').click();
    await d.refrescar();
  });

  it('CA-1-17 Esc cierra el diálogo sin guardar', async () => {
    const d = await abrir();
    d.dialogo()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await d.refrescar();

    expect(await d.resultado).toBeUndefined();
  });
});
