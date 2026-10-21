import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CLAVE_CACHE_UBIGEO, type deliveryApi } from 'shared';
import { montar } from '../../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { abrirRuta, botonDe, filasTabla, respuesta } from '../entregas/datos-prueba';
import { TARIFAS_ROUTES } from './maestros-envio.routes';

vi.setConfig({ testTimeout: 30000 });

const URL = '/api/v1/admin/tarifas';
const UBIGEO = '/api/v1/ubigeo';

const tarifa = (id: number, cambios: Partial<deliveryApi.Tarifa>): deliveryApi.Tarifa => ({
  id,
  modalidad: 'DELIVERY_LOCAL',
  ubigeo: '150122',
  nivel: 'DISTRITO',
  costo: 10,
  plazoDiasMin: 1,
  plazoDiasMax: 2,
  activa: true,
  ...cambios,
});
const MIRAFLORES = tarifa(1, {});
const LIMA = tarifa(2, { ubigeo: '1501', nivel: 'PROVINCIA', costo: 12 });
const AREQUIPA = tarifa(3, {
  modalidad: 'AGENCIA',
  ubigeo: '04',
  nivel: 'DEPARTAMENTO',
  costo: 18,
  plazoDiasMin: 2,
  plazoDiasMax: 3,
});

const NOMBRES: Record<string, object> = {
  '150122': {
    codigo: '150122',
    nombre: 'Miraflores',
    departamento: 'Lima',
    provincia: 'Lima',
    nombreCompleto: 'Miraflores, Lima, Lima',
  },
  '1501': { codigo: '1501', nombre: 'Lima', departamento: 'Lima', nombreCompleto: 'Lima, Lima' },
  '04': { codigo: '04', nombre: 'Arequipa', nombreCompleto: 'Arequipa' },
};

/** Listas de ubigeo que responde el servidor simulado: las de Arequipa y las de Lima Metropolitana. */
const LISTAS: Record<string, object[]> = {
  departamentos: [
    { codigo: '04', nombre: 'Arequipa' },
    { codigo: '15', nombre: 'Lima' },
  ],
  'departamentos/15/provincias': [{ codigo: '1501', nombre: 'Lima' }],
  'provincias/1501/distritos': [{ codigo: '150122', nombre: 'Miraflores' }],
};

async function abrir() {
  localStorage.removeItem(CLAVE_CACHE_UBIGEO);
  const m = await abrirRuta('tarifas', TARIFAS_ROUTES, '/tarifas', 'ADMIN');
  const lista = () => m.ctrl.expectOne((r) => r.url === URL && r.method === 'GET');
  /** Responde cada consulta pendiente de ubigeo: los nombres de los destinos (`/ubigeo/{código}`) o las listas. */
  const ubigeo = async (listas: Record<string, object[]> = LISTAS) => {
    for (let i = 0; i < 4; i++) {
      for (const p of m.ctrl.match((r) => r.url.startsWith(UBIGEO) && r.method === 'GET')) {
        const resto = p.request.url.slice(UBIGEO.length + 1);
        p.flush(respuesta(listas[resto] ?? NOMBRES[resto] ?? []));
      }
      await m.estabilizar();
    }
  };
  /** Responde la lista de tarifas y los nombres de sus destinos. */
  const cargar = async (filas: deliveryApi.Tarifa[]) => {
    lista().flush(respuesta(filas));
    await m.estabilizar();
    await ubigeo();
  };
  return { ...m, lista, cargar, ubigeo };
}

const elegir = async (
  m: { estabilizar(): Promise<void> },
  select: HTMLSelectElement,
  valor: string,
) => {
  select.value = valor;
  select.dispatchEvent(new Event('change'));
  await m.estabilizar();
};

describe('TarifasPage (P-6-05)', () => {
  it('CA-6-07 la lista muestra el destino con sus nombres, el costo, el plazo y el nivel', async () => {
    const m = await abrir();
    await m.cargar([MIRAFLORES, LIMA]);
    const filas = filasTabla(m.el());

    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('Lima › Lima › Miraflores');
    expect(filas[0].textContent).toContain('S/ 10.00');
    expect(filas[0].textContent).toContain('1–2 días');
    expect(filas[1].textContent).toContain('Lima › Lima (provincia)');
  });

  it('CA-6-07 la pestaña Agencia pide solo esa modalidad y muestra "Arequipa (departamento)"', async () => {
    const m = await abrir();
    expect(m.lista().request.params.get('modalidad')).toBe('DELIVERY_LOCAL');
    botonDe(m.el(), 'Agencia').click();
    await m.estabilizar();

    const p = m.ctrl.match((r) => r.url === URL && r.method === 'GET');
    expect(p.length).toBe(1);
    expect(p[0].request.params.get('modalidad')).toBe('AGENCIA');
    p[0].flush(respuesta([AREQUIPA]));
    await m.estabilizar();
    await m.ubigeo();

    expect(filasTabla(m.el())[0].textContent).toContain('Arequipa (departamento)');
  });

  it('CA-6-07 "Nueva tarifa" ofrece los tres niveles y guarda el código del nivel elegido', async () => {
    const m = await abrir();
    await m.cargar([]);

    botonDe(m.el(), 'Nueva tarifa').click();
    await m.estabilizar();
    await m.ubigeo();
    const nivel = m.panel().querySelector('#nivel-tarifa') as HTMLSelectElement;
    expect([...nivel.options].map((o) => o.value)).toEqual([
      'distrito',
      'provincia',
      'departamento',
    ]);

    await elegir(m, m.panel().querySelector('#modalidad-tarifa') as HTMLSelectElement, 'AGENCIA');
    await elegir(m, nivel, 'departamento');
    // A nivel departamento el selector de ubigeo solo tiene el select de departamento
    expect(m.panel().querySelectorAll('fs-selector-ubigeo select').length).toBe(1);
    await m.ubigeo();
    await elegir(
      m,
      m.panel().querySelector('fs-selector-ubigeo select') as HTMLSelectElement,
      '04',
    );

    const campos = m.panel().querySelectorAll<HTMLInputElement>('fs-campo-formulario input');
    ['18.5', '2', '3'].forEach((v, i) => {
      campos[i].value = v;
      campos[i].dispatchEvent(new Event('input'));
    });
    await m.estabilizar();
    m.botonPanel('Guardar').click();
    await m.estabilizar();

    const post = await m.esperar((r) => r.url === URL && r.method === 'POST');
    expect((post as unknown as { request: { body: unknown } }).request.body).toEqual({
      modalidad: 'AGENCIA',
      ubigeo: '04',
      costo: 18.5,
      plazoDiasMin: 2,
      plazoDiasMax: 3,
      activa: true,
    });
    post.flush(respuesta({ ...AREQUIPA, costo: 18.5 }));
    (await m.esperar((r) => r.url === URL && r.method === 'GET')).flush(respuesta([]));
    await m.estabilizar();
  });

  it('CA-6-08 repetir modalidad y destino muestra el mensaje del servidor en el campo del destino', async () => {
    const m = await abrir();
    await m.cargar([]);

    botonDe(m.el(), 'Nueva tarifa').click();
    await m.estabilizar();
    await m.ubigeo();
    await elegir(m, m.panel().querySelector('#nivel-tarifa') as HTMLSelectElement, 'departamento');
    await m.ubigeo();
    await elegir(
      m,
      m.panel().querySelector('fs-selector-ubigeo select') as HTMLSelectElement,
      '04',
    );
    const campos = m.panel().querySelectorAll<HTMLInputElement>('fs-campo-formulario input');
    ['18', '2', '3'].forEach((v, i) => {
      campos[i].value = v;
      campos[i].dispatchEvent(new Event('input'));
    });
    await m.estabilizar();
    m.botonPanel('Guardar').click();

    (await m.esperar((r) => r.url === URL && r.method === 'POST')).flush(
      {
        success: false,
        mensaje: 'Ya existe una tarifa para ese destino',
        codigo: 'TARIFA_DUPLICADA',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await m.hasta(() => !!m.panel().textContent?.includes('Ya existe una tarifa para ese destino'));

    expect(m.panel().textContent).toContain('Ya existe una tarifa para ese destino');
  });

  it('CA-6-09 el plazo mínimo mayor que el máximo se corrige antes de enviar', async () => {
    const m = await abrir();
    await m.cargar([]);

    botonDe(m.el(), 'Nueva tarifa').click();
    await m.estabilizar();
    await m.ubigeo();
    const campos = m.panel().querySelectorAll<HTMLInputElement>('fs-campo-formulario input');
    ['9', '5', '2'].forEach((v, i) => {
      campos[i].value = v;
      campos[i].dispatchEvent(new Event('input'));
    });
    await m.estabilizar();
    m.botonPanel('Guardar').click();
    await m.estabilizar();

    expect(m.panel().textContent).toContain('El plazo máximo no puede ser menor que el mínimo');
    expect(m.ctrl.match((r) => r.url === URL && r.method === 'POST').length).toBe(0);
  });

  it('CA-6-10 desactivar vuelve a guardar la tarifa como inactiva y eliminar pide confirmación', async () => {
    const m = await abrir();
    await m.cargar([MIRAFLORES]);

    botonDe(filasTabla(m.el())[0], 'Desactivar').click();
    await m.estabilizar();
    const put = m.ctrl.expectOne((r) => r.url === `${URL}/1` && r.method === 'PUT');
    expect(put.request.body).toMatchObject({ ubigeo: '150122', activa: false });
    put.flush(respuesta({ ...MIRAFLORES, activa: false }));
    await m.estabilizar();
    await m.cargar([{ ...MIRAFLORES, activa: false }]);
    expect(filasTabla(m.el())[0].textContent).toContain('Inactiva');

    botonDe(filasTabla(m.el())[0], 'Eliminar').click();
    await m.estabilizar();
    m.botonPanel('Eliminar').click();
    (await m.esperar((r) => r.url === `${URL}/1` && r.method === 'DELETE')).flush(respuesta(null));
    (await m.esperar((r) => r.url === URL && r.method === 'GET')).flush(respuesta([]));
    await m.estabilizar();
  });

  it('CA-6-11 "Probar cotización" muestra la tarifa que se aplicaría: nivel, costo y plazo', async () => {
    const m = await abrir();
    await m.cargar([]);
    const prueba = () =>
      m.el().querySelector('section[aria-label="Probar cotización"]') as HTMLElement;

    await m.ubigeo();
    const selects = () => prueba().querySelectorAll('select');
    await elegir(m, selects()[0] as HTMLSelectElement, '15');
    await m.ubigeo();
    await elegir(m, selects()[1] as HTMLSelectElement, '1501');
    await m.ubigeo();
    await elegir(m, selects()[2] as HTMLSelectElement, '150122');
    botonDe(prueba(), 'Probar con delivery local').click();
    await m.estabilizar();

    const cotizar = m.ctrl.expectOne((r) => r.url === '/api/v1/envios/cotizar');
    expect(cotizar.request.body).toEqual({ modalidad: 'DELIVERY_LOCAL', ubigeo: '150122' });
    cotizar.flush(
      respuesta({
        costo: 10,
        plazoDiasMin: 1,
        plazoDiasMax: 2,
        agenciaNombre: null,
        nivelTarifa: 'PROVINCIA',
        ubigeoTarifa: '1501',
      }),
    );
    await m.estabilizar();

    const texto = m.el().querySelector('[data-testid="resultado-cotizacion"]')?.textContent ?? '';
    expect(texto).toContain('provincia (1501)');
    expect(texto).toContain('S/ 10.00');
    expect(texto).toContain('1–2 días');
  });

  it('CA-6-11 si no hay tarifa para el destino muestra "Sin cobertura"', async () => {
    const m = await abrir();
    await m.cargar([]);
    const prueba = () =>
      m.el().querySelector('section[aria-label="Probar cotización"]') as HTMLElement;

    await m.ubigeo();
    await elegir(m, prueba().querySelectorAll('select')[0] as HTMLSelectElement, '15');
    await m.ubigeo();
    await elegir(m, prueba().querySelectorAll('select')[1] as HTMLSelectElement, '1501');
    await m.ubigeo();
    await elegir(m, prueba().querySelectorAll('select')[2] as HTMLSelectElement, '150122');
    botonDe(m.el(), 'Agencia').click();
    await m.estabilizar();
    m.ctrl.match((r) => r.url === URL && r.method === 'GET').forEach((p) => p.flush(respuesta([])));
    await m.estabilizar();
    botonDe(prueba(), 'Probar con agencia').click();
    await m.estabilizar();

    m.ctrl
      .expectOne((r) => r.url === '/api/v1/envios/cotizar')
      .flush(
        {
          success: false,
          mensaje: 'No realizamos envíos por esta modalidad a este distrito',
          codigo: 'SIN_COBERTURA',
        },
        { status: 422, statusText: 'Unprocessable' },
      );
    await m.estabilizar();

    expect(m.el().querySelector('[data-testid="resultado-cotizacion"]')?.textContent).toContain(
      'Sin cobertura',
    );
  });
});

describe('Tarifas en el backoffice (CA-6-12)', () => {
  it('CA-6-12 un OPERADOR que abre /tarifas es llevado a la página 403', async () => {
    const h = await montar(SESION_OPERADOR, { url: '/' });
    await h.navigateByUrl('/tarifas');
    expect(TestBed.inject(Router).url).toContain('403');
  });

  it('CA-6-12 un ADMIN sí entra a /tarifas y a /agencias', async () => {
    const h = await montar(SESION_ADMIN, { url: '/' });
    await h.navigateByUrl('/tarifas');
    expect(TestBed.inject(Router).url).toBe('/tarifas');
    await h.navigateByUrl('/agencias');
    expect(TestBed.inject(Router).url).toBe('/agencias');
  });
});
