import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideFitstoreHttp } from '../http/proveedores';
import { CLAVE_CACHE_UBIGEO, UbigeoCatalogo } from './ubigeo-catalogo';
import { departamentoDe, nivelDe, provinciaDe } from './codigos';

const DEPARTAMENTOS = [
  { codigo: '04', nombre: 'Arequipa' },
  { codigo: '15', nombre: 'Lima' },
];

function respuesta<T>(data: T) {
  return { success: true, mensaje: 'Operación exitosa', data };
}

function configurar() {
  TestBed.configureTestingModule({
    providers: [provideFitstoreHttp({ carrito: false }), provideHttpClientTesting()],
  });
  return { catalogo: TestBed.inject(UbigeoCatalogo), ctrl: TestBed.inject(HttpTestingController) };
}

describe('Códigos de ubigeo', () => {
  it('CA-0b-26 Lima › Lima › Miraflores es el código 150122 y de él salen sus niveles', () => {
    expect(departamentoDe('150122')).toBe('15');
    expect(provinciaDe('150122')).toBe('1501');
    expect(nivelDe('15')).toBe('departamento');
    expect(nivelDe('1501')).toBe('provincia');
    expect(nivelDe('150122')).toBe('distrito');
  });

  it('CA-0b-26 con hasta = "departamento" o "provincia" el código emitido es 04 o 0401', () => {
    expect(departamentoDe('040101')).toBe('04');
    expect(provinciaDe('040101')).toBe('0401');
  });

  it('CA-0b-26 un código con formato inválido no tiene nivel', () => {
    expect(nivelDe('1')).toBeNull();
    expect(nivelDe('abcdef')).toBeNull();
    expect(nivelDe('15012')).toBeNull();
  });
});

describe('UbigeoCatalogo', () => {
  beforeEach(() => localStorage.clear());

  it('CA-0b-26 las listas encadenadas vienen de /ubigeo: departamentos, provincias y distritos', () => {
    const { catalogo, ctrl } = configurar();
    const recibido: Record<string, unknown> = {};

    catalogo.departamentos().subscribe((v) => (recibido['dep'] = v));
    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
    catalogo.provincias('15').subscribe((v) => (recibido['prov'] = v));
    ctrl
      .expectOne('/api/v1/ubigeo/departamentos/15/provincias')
      .flush(respuesta([{ codigo: '1501', nombre: 'Lima' }]));
    catalogo.distritos('1501').subscribe((v) => (recibido['dist'] = v));
    ctrl
      .expectOne('/api/v1/ubigeo/provincias/1501/distritos')
      .flush(respuesta([{ codigo: '150122', nombre: 'Miraflores' }]));

    expect(recibido['dep']).toEqual(DEPARTAMENTOS);
    expect(recibido['prov']).toEqual([{ codigo: '1501', nombre: 'Lima' }]);
    expect(recibido['dist']).toEqual([{ codigo: '150122', nombre: 'Miraflores' }]);
  });

  it('CA-0b-26 la segunda consulta sale de la caché: no vuelve a llamar a la API', () => {
    const { catalogo, ctrl } = configurar();

    catalogo.departamentos().subscribe();
    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
    let segunda: unknown;
    catalogo.departamentos().subscribe((v) => (segunda = v));

    ctrl.expectNone('/api/v1/ubigeo/departamentos');
    expect(segunda).toEqual(DEPARTAMENTOS);
  });

  it('CA-0b-26 consultas simultáneas comparten una sola llamada', () => {
    const { catalogo, ctrl } = configurar();

    catalogo.departamentos().subscribe();
    catalogo.departamentos().subscribe();

    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
  });

  it('CA-0b-26 la caché sobrevive al recargar la página (localStorage del navegador)', () => {
    const primera = configurar();
    primera.catalogo.departamentos().subscribe();
    primera.ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
    expect(localStorage.getItem(CLAVE_CACHE_UBIGEO)).toContain('Arequipa');

    TestBed.resetTestingModule();
    const segunda = configurar();
    let lista: unknown;
    segunda.catalogo.departamentos().subscribe((v) => (lista = v));

    segunda.ctrl.expectNone('/api/v1/ubigeo/departamentos');
    expect(lista).toEqual(DEPARTAMENTOS);
  });

  it('CA-0b-26 una caché dañada se ignora y se vuelve a pedir', () => {
    localStorage.setItem(CLAVE_CACHE_UBIGEO, '{roto');
    const { catalogo, ctrl } = configurar();

    catalogo.departamentos().subscribe();

    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
  });

  it('CA-0b-26 un error no se guarda en la caché: la siguiente consulta reintenta', () => {
    const { catalogo, ctrl } = configurar();

    catalogo.departamentos().subscribe({ error: () => undefined });
    ctrl
      .expectOne('/api/v1/ubigeo/departamentos')
      .flush({ success: false, mensaje: 'x', codigo: 'X' }, { status: 500, statusText: 'x' });
    catalogo.departamentos().subscribe();

    ctrl.expectOne('/api/v1/ubigeo/departamentos').flush(respuesta(DEPARTAMENTOS));
  });

  it('CA-0b-26 el buscador por nombre ("miraf") devuelve el distrito con su provincia y departamento', () => {
    const { catalogo, ctrl } = configurar();
    let resultado: unknown;

    catalogo.buscarDistritos('miraf').subscribe((v) => (resultado = v));
    const req = ctrl.expectOne((r) => r.url === '/api/v1/ubigeo/distritos');
    expect(req.request.params.get('q')).toBe('miraf');
    const encontrados = [
      { codigo: '150122', nombre: 'Miraflores', provincia: 'Lima', departamento: 'Lima' },
      { codigo: '040129', nombre: 'Miraflores', provincia: 'Arequipa', departamento: 'Arequipa' },
    ];
    req.flush(respuesta(encontrados));

    expect(resultado).toEqual(encontrados);
  });

  it('CA-0b-26 el nombre completo de un código ("Miraflores, Lima, Lima") también se guarda en la caché', () => {
    const { catalogo, ctrl } = configurar();
    const nombre = {
      codigo: '150122',
      nombre: 'Miraflores',
      nombreCompleto: 'Miraflores, Lima, Lima',
    };

    catalogo.nombre('150122').subscribe();
    ctrl.expectOne('/api/v1/ubigeo/150122').flush(respuesta(nombre));
    let segunda: unknown;
    catalogo.nombre('150122').subscribe((v) => (segunda = v));

    ctrl.expectNone('/api/v1/ubigeo/150122');
    expect(segunda).toEqual(nombre);
  });
});
