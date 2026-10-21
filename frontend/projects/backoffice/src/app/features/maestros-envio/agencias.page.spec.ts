import type { deliveryApi } from 'shared';
import { abrirRuta, botonDe, respuesta } from '../entregas/datos-prueba';
import { AGENCIAS_ROUTES } from './maestros-envio.routes';

vi.setConfig({ testTimeout: 30000 });

const URL = '/api/v1/admin/agencias';

const agencia = (
  id: number,
  nombre: string,
  orden: number,
  activa = true,
): deliveryApi.Agencia => ({
  id,
  nombre,
  urlSeguimiento: `https://rastreo.ejemplo.pe/${nombre.toLowerCase()}?codigo={codigo}`,
  activa,
  orden,
});
const TODAS = [agencia(1, 'Shalom', 0), agencia(2, 'Olva', 1), agencia(3, 'Marvisur', 2, false)];

async function abrir() {
  const m = await abrirRuta('agencias', AGENCIAS_ROUTES, '/agencias', 'ADMIN');
  const lista = () => m.ctrl.expectOne((r) => r.url === URL && r.method === 'GET');
  const cargar = async (filas = TODAS) => {
    lista().flush(respuesta(filas));
    await m.estabilizar();
  };
  const filas = () => [...m.el().querySelectorAll<HTMLElement>('[data-testid="fila-agencia"]')];
  const fila = (nombre: string) => filas().find((f) => f.textContent?.includes(nombre))!;
  return { ...m, lista, cargar, filas, fila };
}

describe('AgenciasPage (P-6-06)', () => {
  it('CA-6-14 lista las agencias en su orden, con su estado', async () => {
    const m = await abrir();
    await m.cargar();

    expect(m.filas().map((f) => f.textContent)).toEqual([
      expect.stringContaining('Shalom'),
      expect.stringContaining('Olva'),
      expect.stringContaining('Marvisur'),
    ]);
    expect(m.fila('Marvisur').textContent).toContain('Inactiva');
    expect(botonDe(m.fila('Marvisur'), 'Activar')).toBeTruthy();
    expect(botonDe(m.fila('Shalom'), 'Desactivar')).toBeTruthy();
  });

  it('CA-6-14 bajar una agencia manda su nueva posición y muestra el orden que devuelve el servidor', async () => {
    const m = await abrir();
    await m.cargar();

    (
      m.fila('Shalom').querySelector('button[aria-label="Bajar Shalom"]') as HTMLButtonElement
    ).click();
    await m.estabilizar();
    const patch = m.ctrl.expectOne((r) => r.url === `${URL}/1/orden` && r.method === 'PATCH');
    expect(patch.request.body).toEqual({ orden: 1 });
    patch.flush(
      respuesta([agencia(2, 'Olva', 0), agencia(1, 'Shalom', 1), agencia(3, 'Marvisur', 2, false)]),
    );
    await m.estabilizar();

    expect(m.filas()[0].textContent).toContain('Olva');
    expect(m.filas()[1].textContent).toContain('Shalom');
  });

  it('CA-6-14 desactivar y activar llaman a su endpoint y vuelven a leer la lista', async () => {
    const m = await abrir();
    await m.cargar();

    botonDe(m.fila('Shalom'), 'Desactivar').click();
    await m.estabilizar();
    m.ctrl
      .expectOne((r) => r.url === `${URL}/1/desactivar` && r.method === 'PATCH')
      .flush(respuesta({ ...TODAS[0], activa: false }));
    await m.estabilizar();
    await m.cargar([{ ...TODAS[0], activa: false }, TODAS[1], TODAS[2]]);

    botonDe(m.fila('Marvisur'), 'Activar').click();
    await m.estabilizar();
    m.ctrl
      .expectOne((r) => r.url === `${URL}/3/activar` && r.method === 'PATCH')
      .flush(respuesta({ ...TODAS[2], activa: true }));
    await m.estabilizar();
    m.lista().flush(respuesta(TODAS));
  });

  it('CA-6-13 el diálogo valida la URL de rastreo y muestra la vista previa con un código de ejemplo', async () => {
    const m = await abrir();
    await m.cargar();

    botonDe(m.el(), 'Nueva agencia').click();
    await m.estabilizar();
    const campos = () => m.panel().querySelectorAll<HTMLInputElement>('fs-campo-formulario input');
    const escribir = async (i: number, v: string) => {
      campos()[i].value = v;
      campos()[i].dispatchEvent(new Event('input'));
      await m.estabilizar();
    };
    await escribir(0, 'Transportes Andes');
    await escribir(1, 'http://andes.pe/rastreo');
    m.botonPanel('Guardar').click();
    await m.estabilizar();
    expect(m.panel().textContent).toContain('La URL debe ser https e incluir {codigo}');
    expect(m.ctrl.match((r) => r.url === URL && r.method === 'POST').length).toBe(0);

    await escribir(1, 'https://andes.pe/rastreo?codigo={codigo}');
    expect(m.panel().querySelector('[data-testid="vista-previa"]')?.textContent).toContain(
      'https://andes.pe/rastreo?codigo=ABC123',
    );
    m.botonPanel('Guardar').click();
    const post = await m.esperar((r) => r.url === URL && r.method === 'POST');
    expect((post as unknown as { request: { body: unknown } }).request.body).toEqual({
      nombre: 'Transportes Andes',
      urlSeguimiento: 'https://andes.pe/rastreo?codigo={codigo}',
    });
    post.flush(respuesta(agencia(9, 'Transportes Andes', 3)));
    (await m.esperar((r) => r.url === URL && r.method === 'GET')).flush(respuesta(TODAS));
    await m.estabilizar();
  });

  it('CA-6-15 eliminar una agencia con entregas (409) muestra el motivo y vuelve a leer la lista', async () => {
    const m = await abrir();
    await m.cargar();

    botonDe(m.fila('Olva'), 'Eliminar').click();
    await m.estabilizar();
    m.botonPanel('Eliminar').click();
    (await m.esperar((r) => r.url === `${URL}/2` && r.method === 'DELETE')).flush(
      {
        success: false,
        mensaje: 'La agencia tiene entregas; desactívala en vez de eliminarla',
        codigo: 'AGENCIA_CON_ENTREGAS',
      },
      { status: 409, statusText: 'Conflict' },
    );
    (await m.esperar((r) => r.url === URL && r.method === 'GET')).flush(respuesta(TODAS));
    await m.estabilizar();

    expect(m.filas().length).toBe(3);
  });

  it('CA-6-14 sin agencias muestra el estado vacío', async () => {
    const m = await abrir();
    await m.cargar([]);

    expect(m.el().textContent).toContain('Sin agencias');
  });
});
