import { TestBed } from '@angular/core/testing';
import { authApi } from 'shared';
import { montar } from '../../../testing';
import { PROVEEDORES, http, ok } from '../pruebas-cuenta';
import { DialogoDireccionService } from './dialogo-direccion.service';
import { DireccionesPage } from './direcciones.page';

const RUTA = '/api/v1/clientes/me/direcciones';

function dir(id: number, alias: string, predeterminada = false): authApi.Direccion {
  return {
    id,
    alias,
    ubigeo: '150115',
    direccion: `Jr. ${alias} ${id}00`,
    referencia: null,
    telefonoContacto: null,
    predeterminada,
  };
}

async function iniciar(lista: authApi.Direccion[]) {
  const m = await montar(DireccionesPage, {}, PROVEEDORES);
  http().expectOne(RUTA).flush(ok(lista));
  await m.estabilizar();
  const boton = (texto: string) =>
    [...m.el.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  const tarjetas = () => [...m.el.querySelectorAll<HTMLElement>('fs-tarjeta-direccion')];
  return { ...m, boton, tarjetas };
}

describe('DireccionesPage (P-1-06)', () => {
  afterEach(() => http().verify());

  it('CA-1-17 sin direcciones muestra "Aún no tienes direcciones" y permite agregar la primera', async () => {
    const p = await iniciar([]);

    expect(p.el.textContent).toContain('Aún no tienes direcciones');
    expect(p.boton('Agregar dirección').disabled).toBe(false);
  });

  it('CA-1-17 lista las direcciones con su alias y marca la predeterminada', async () => {
    const p = await iniciar([dir(1, 'Casa', true), dir(2, 'Trabajo')]);

    expect(p.tarjetas()).toHaveLength(2);
    expect(p.tarjetas()[0].textContent).toContain('Casa');
    expect(p.tarjetas()[0].textContent).toContain('Predeterminada');
    expect(p.tarjetas()[1].textContent).not.toContain('Predeterminada');
    expect(p.tarjetas()[1].textContent).toContain('Marcar como predeterminada');
  });

  it('CA-1-18 "Marcar como predeterminada" llama a PATCH y refleja la lista que devuelve', async () => {
    const p = await iniciar([dir(1, 'Casa', true), dir(2, 'Trabajo')]);
    [...p.tarjetas()[1].querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Marcar como predeterminada'))!
      .click();

    const peticion = http().expectOne(`${RUTA}/2/predeterminada`);
    expect(peticion.request.method).toBe('PATCH');
    peticion.flush(ok([dir(2, 'Trabajo', true), dir(1, 'Casa')]));
    await p.estabilizar();

    expect(p.tarjetas()[0].textContent).toContain('Trabajo');
    expect(p.tarjetas()[0].textContent).toContain('Predeterminada');
  });

  it('CA-1-19 eliminar pide confirmación y luego borra la dirección', async () => {
    const p = await iniciar([dir(1, 'Casa', true), dir(2, 'Trabajo')]);
    [...p.tarjetas()[1].querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Eliminar'))!
      .click();
    await p.estabilizar();

    expect(p.el.querySelector('[role="dialog"]')?.textContent).toContain(
      '¿Eliminar la dirección Trabajo?',
    );
    http().expectNone(`${RUTA}/2`);
    [...p.el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')]
      .find((b) => b.textContent?.includes('Eliminar dirección'))!
      .click();
    await p.estabilizar();

    const borrado = http().expectOne(`${RUTA}/2`);
    expect(borrado.request.method).toBe('DELETE');
    borrado.flush(ok(null, 'Dirección eliminada'));
    await p.estabilizar();
    http()
      .expectOne(RUTA)
      .flush(ok([dir(1, 'Casa', true)]));
    await p.estabilizar();

    expect(p.tarjetas()).toHaveLength(1);
  });

  it('CA-1-19 si se cancela la confirmación no se borra nada', async () => {
    const p = await iniciar([dir(1, 'Casa', true)]);
    p.tarjetas()[0].querySelectorAll<HTMLButtonElement>('button')[1].click();
    await p.estabilizar();
    [...p.el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')]
      .find((b) => b.textContent?.includes('Volver'))!
      .click();
    await p.estabilizar();

    expect(p.el.querySelector('[role="dialog"]')).toBeNull();
    http().expectNone(`${RUTA}/1`);
  });

  it('CA-1-20 con 10 direcciones el botón "Agregar dirección" queda deshabilitado y explica el límite', async () => {
    const lista = Array.from({ length: 10 }, (_, i) => dir(i + 1, `Dirección ${i + 1}`, i === 0));
    const p = await iniciar(lista);

    expect(p.boton('Agregar dirección').disabled).toBe(true);
    expect(p.el.textContent).toContain('máximo de 10 direcciones');
  });

  it('CA-1-17 agregar abre el diálogo y, si se guardó una dirección, recarga la lista', async () => {
    const p = await iniciar([]);
    const abrir = vi
      .spyOn(TestBed.inject(DialogoDireccionService), 'abrir')
      .mockResolvedValue(dir(1, 'Casa', true));
    p.boton('Agregar dirección').click();
    await p.estabilizar();
    await Promise.resolve();

    expect(abrir).toHaveBeenCalledWith(undefined);
    http()
      .expectOne(RUTA)
      .flush(ok([dir(1, 'Casa', true)]));
    await p.estabilizar();
    expect(p.tarjetas()).toHaveLength(1);
  });

  it('CA-1-19 editar abre el diálogo con la dirección; si se cancela no recarga', async () => {
    const p = await iniciar([dir(1, 'Casa', true)]);
    const abrir = vi
      .spyOn(TestBed.inject(DialogoDireccionService), 'abrir')
      .mockResolvedValue(undefined);
    [...p.tarjetas()[0].querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Editar'))!
      .click();
    await p.estabilizar();
    await Promise.resolve();

    expect(abrir).toHaveBeenCalledWith(expect.objectContaining({ id: 1, alias: 'Casa' }));
    http().expectNone(RUTA);
  });

  it('CA-1-17 si no se pueden cargar las direcciones muestra el error con "Reintentar"', async () => {
    const m = await montar(DireccionesPage, {}, PROVEEDORES);
    http().expectOne(RUTA).flush(null, { status: 500, statusText: 'Error' });
    await m.estabilizar();

    expect(m.el.querySelector('[role="alert"]')?.textContent).toContain('Reintentar');
    [...m.el.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Reintentar'))!
      .click();
    http().expectOne(RUTA).flush(ok([]));
    await m.estabilizar();
    expect(m.el.textContent).toContain('Aún no tienes direcciones');
  });
});
