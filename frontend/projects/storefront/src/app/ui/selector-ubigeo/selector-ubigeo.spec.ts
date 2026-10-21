import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideFitstoreHttp } from 'shared';
import { FsSelectorUbigeo } from './selector-ubigeo';
import { montar } from '../../testing';

const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });
const DEPARTAMENTOS = [
  { codigo: '04', nombre: 'Arequipa' },
  { codigo: '15', nombre: 'Lima' },
];
const PROVINCIAS = [
  { codigo: '1501', nombre: 'Lima' },
  { codigo: '1502', nombre: 'Barranca' },
];
const DISTRITOS = [
  { codigo: '150101', nombre: 'Lima' },
  { codigo: '150122', nombre: 'Miraflores' },
];

async function iniciar(entradas: Record<string, unknown> = {}) {
  localStorage.clear();
  const m = await montar(FsSelectorUbigeo, entradas, [
    provideFitstoreHttp({ carrito: false }),
    provideHttpClientTesting(),
  ]);
  const ctrl = TestBed.inject(HttpTestingController);
  const emitidos: (string | null)[] = [];
  m.comp.cambio.subscribe((c: string | null) => emitidos.push(c));
  const selects = () => [...m.el.querySelectorAll('select')] as HTMLSelectElement[];
  const elegir = async (indice: number, valor: string) => {
    const s = selects()[indice];
    s.value = valor;
    s.dispatchEvent(new Event('change'));
    await m.estabilizar();
  };
  const responder = async (url: string, datos: unknown) => {
    ctrl.expectOne(url).flush(ok(datos));
    await m.estabilizar();
  };
  return { ...m, ctrl, emitidos, selects, elegir, responder };
}

/** El buscador espera 300 ms sin teclear antes de consultar. */
const esperarBuscador = () => new Promise((r) => setTimeout(r, 350));

/** Lo justo de `process` de Node para escuchar errores sin manejar (el proyecto no trae @types/node). */
interface NodeLike {
  on(evento: string, f: (e: unknown) => void): void;
  off(evento: string, f: (e: unknown) => void): void;
}

describe('Selector de ubigeo de ui/', () => {
  it('CA-0b-26 si /ubigeo falla, el selector queda vacío y no deja un error sin manejar', async () => {
    const u = await iniciar();
    const errores: unknown[] = [];
    const capturar = (e: unknown) => errores.push(e);
    const proc = (globalThis as unknown as { process: NodeLike }).process;
    proc.on('uncaughtException', capturar);
    u.ctrl
      .expectOne('/api/v1/ubigeo/departamentos')
      .flush({ success: false, mensaje: 'Error', codigo: 'ERROR' }, { status: 500, statusText: 'Error' });
    await u.estabilizar();
    await new Promise((r) => setTimeout(r, 0));
    proc.off('uncaughtException', capturar);
    expect(errores).toEqual([]);
    expect(u.selects()[0].options.length).toBe(1);
  });

  it('CA-0b-26 el buscador espera a que se deje de escribir: una sola consulta con el texto final', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    const buscador = u.el.querySelector('input[type="search"]') as HTMLInputElement;
    for (const texto of ['mi', 'mir', 'miraf']) {
      buscador.value = texto;
      buscador.dispatchEvent(new Event('input'));
    }
    await u.estabilizar();
    u.ctrl.expectNone((r) => r.url === '/api/v1/ubigeo/distritos');
    await esperarBuscador();
    await u.estabilizar();
    const peticiones = u.ctrl.match((r) => r.url === '/api/v1/ubigeo/distritos');
    expect(peticiones.map((p) => p.request.params.get('q'))).toEqual(['miraf']);
  });

  it('CA-0b-26 elegir Lima › Lima › Miraflores emite 150122', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    await u.elegir(0, '15');
    await u.responder('/api/v1/ubigeo/departamentos/15/provincias', PROVINCIAS);
    await u.elegir(1, '1501');
    await u.responder('/api/v1/ubigeo/provincias/1501/distritos', DISTRITOS);
    await u.elegir(2, '150122');
    expect(u.emitidos.at(-1)).toBe('150122');
  });

  it('CA-0b-26 las etiquetas son visibles y los tres selects están en el DOM', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    expect(u.selects()).toHaveLength(3);
    expect(u.el.textContent).toContain('Departamento');
    expect(u.el.textContent).toContain('Provincia');
    expect(u.el.textContent).toContain('Distrito');
    expect(u.selects()[1].disabled).toBe(true);
  });

  it('CA-0b-26 elegir un departamento vacía provincia y distrito', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    await u.elegir(0, '15');
    await u.responder('/api/v1/ubigeo/departamentos/15/provincias', PROVINCIAS);
    await u.elegir(1, '1501');
    await u.responder('/api/v1/ubigeo/provincias/1501/distritos', DISTRITOS);
    await u.elegir(2, '150122');
    await u.elegir(0, '04');
    u.ctrl.expectOne('/api/v1/ubigeo/departamentos/04/provincias');
    expect(u.selects()[1].value).toBe('');
    expect(u.selects()[2].value).toBe('');
    expect(u.emitidos.at(-1)).toBeNull();
  });

  it('CA-0b-26 con hasta = "departamento" solo hay un select y emite 04', async () => {
    const u = await iniciar({ hasta: 'departamento' });
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    expect(u.selects()).toHaveLength(1);
    await u.elegir(0, '04');
    expect(u.emitidos.at(-1)).toBe('04');
    u.ctrl.expectNone('/api/v1/ubigeo/departamentos/04/provincias');
  });

  it('CA-0b-26 con hasta = "provincia" se detiene en la provincia y emite 0401', async () => {
    const u = await iniciar({ hasta: 'provincia' });
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    await u.elegir(0, '04');
    await u.responder('/api/v1/ubigeo/departamentos/04/provincias', [
      { codigo: '0401', nombre: 'Arequipa' },
    ]);
    expect(u.selects()).toHaveLength(2);
    await u.elegir(1, '0401');
    expect(u.emitidos.at(-1)).toBe('0401');
    u.ctrl.expectNone('/api/v1/ubigeo/provincias/0401/distritos');
  });

  it('CA-0b-26 por defecto llega hasta el distrito', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    expect(u.selects()).toHaveLength(3);
  });

  it('CA-0b-26 se puede buscar el distrito por nombre y llena los tres selects', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    const buscador = u.el.querySelector('input[type="search"]') as HTMLInputElement;
    buscador.value = 'miraf';
    buscador.dispatchEvent(new Event('input'));
    await esperarBuscador();
    await u.estabilizar();
    const peticion = u.ctrl.expectOne((r) => r.url === '/api/v1/ubigeo/distritos');
    expect(peticion.request.params.get('q')).toBe('miraf');
    peticion.flush(
      ok([{ codigo: '150122', nombre: 'Miraflores', provincia: 'Lima', departamento: 'Lima' }]),
    );
    await u.estabilizar();
    const resultado = u.el.querySelector('[role="option"]') as HTMLElement;
    expect(resultado.textContent).toContain('Miraflores');
    resultado.click();
    await u.estabilizar();
    u.ctrl.expectOne('/api/v1/ubigeo/departamentos/15/provincias').flush(ok(PROVINCIAS));
    u.ctrl.expectOne('/api/v1/ubigeo/provincias/1501/distritos').flush(ok(DISTRITOS));
    await u.estabilizar();
    expect(u.emitidos.at(-1)).toBe('150122');
    expect(u.selects().map((s) => s.value)).toEqual(['15', '1501', '150122']);
  });

  it('CA-0b-26 con un código inicial muestra los tres niveles ya elegidos', async () => {
    const u = await iniciar({ valor: '150122' });
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    await u.responder('/api/v1/ubigeo/departamentos/15/provincias', PROVINCIAS);
    await u.responder('/api/v1/ubigeo/provincias/1501/distritos', DISTRITOS);
    expect(u.selects().map((s) => s.value)).toEqual(['15', '1501', '150122']);
  });

  it('CA-0b-26 las listas vienen de la caché del navegador en el segundo uso', async () => {
    const u = await iniciar();
    await u.responder('/api/v1/ubigeo/departamentos', DEPARTAMENTOS);
    TestBed.resetTestingModule();
    const m = await montar(FsSelectorUbigeo, {}, [
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
    ]);
    TestBed.inject(HttpTestingController).expectNone('/api/v1/ubigeo/departamentos');
    expect(m.el.querySelectorAll('select option').length).toBeGreaterThan(1);
  });
});
