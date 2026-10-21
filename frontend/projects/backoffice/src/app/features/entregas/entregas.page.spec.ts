import { ENTREGAS_ROUTES } from './entregas.routes';
import {
  abrirRuta,
  botonDe,
  entrega,
  filasTabla,
  pagina,
  REPARTIDOR,
  respuesta,
  resumen,
} from './datos-prueba';

vi.setConfig({ testTimeout: 30000 });

const LISTA = '/api/v1/admin/entregas';
const REPARTIDORES = '/api/v1/admin/repartidores';

const LOCALES = [
  resumen('11', { estado: 'PENDIENTE_ASIGNACION', siguienteAccion: 'ASIGNAR' }),
  resumen('12', {
    estado: 'ASIGNADA',
    siguienteAccion: 'EN_RUTA',
    fechaProgramada: '2026-10-19',
    repartidorNombre: 'Juan Pérez',
    intento: 1,
  }),
  resumen('13', {
    estado: 'ENTREGADA',
    siguienteAccion: null,
    fechaProgramada: '2026-10-18',
    repartidorNombre: 'Juan Pérez',
    intento: 1,
  }),
];
const CONTADORES = { PENDIENTE_ASIGNACION: 1, ASIGNADA: 1, EN_RUTA: 0, FALLIDA: 0, ENTREGADA: 1 };

async function abrir(url = '/entregas') {
  const montaje = await abrirRuta('entregas', ENTREGAS_ROUTES, url);
  const lista = () => montaje.ctrl.expectOne((r) => r.url === LISTA && r.method === 'GET');
  const cargar = async (filas = LOCALES, contadores: Record<string, number> = CONTADORES) => {
    montaje.ctrl.expectOne((r) => r.url === REPARTIDORES).flush(respuesta([REPARTIDOR]));
    lista().flush(respuesta(pagina(filas, contadores)));
    await montaje.estabilizar();
  };
  return { ...montaje, lista, cargar };
}

describe('EntregasPage (P-6-01)', () => {
  it('CA-6-31 abre en Delivery local, primera página, con 20 por página', async () => {
    const { lista, cargar } = await abrir();
    const p = lista();
    expect(p.request.params.get('modalidad')).toBe('DELIVERY_LOCAL');
    expect(p.request.params.get('page')).toBe('0');
    expect(p.request.params.get('size')).toBe('20');
    p.flush(respuesta(pagina([], {})));
    expect(cargar).toBeDefined();
  });

  it('CA-6-31 muestra las entregas con el código como enlace al detalle y los contadores en los chips', async () => {
    const { el, ctrl, estabilizar } = await abrirRuta('entregas', ENTREGAS_ROUTES, '/entregas');
    ctrl.expectOne((r) => r.url === REPARTIDORES).flush(respuesta([REPARTIDOR]));
    ctrl.expectOne((r) => r.url === LISTA).flush(respuesta(pagina(LOCALES, CONTADORES)));
    await estabilizar();

    expect(el().querySelectorAll('[role="tab"]').length).toBe(2);
    expect(filasTabla(el()).length).toBe(3);
    const enlace = filasTabla(el())[0].querySelector('a.fs-codigo') as HTMLAnchorElement;
    expect(enlace.textContent?.trim()).toBe('FS-000011');
    expect(enlace.getAttribute('href')).toBe('/entregas/11');
    expect(el().textContent).toContain('Por asignar');
    expect(el().textContent).toContain('Entregada');
  });

  it('CA-6-31 cada fila ofrece como botón el siguiente paso; las entregadas solo "Ver ›" (E1)', async () => {
    const { el, ctrl, estabilizar } = await abrirRuta('entregas', ENTREGAS_ROUTES, '/entregas');
    ctrl.expectOne((r) => r.url === REPARTIDORES).flush(respuesta([REPARTIDOR]));
    ctrl.expectOne((r) => r.url === LISTA).flush(respuesta(pagina(LOCALES, CONTADORES)));
    await estabilizar();
    const [pendiente, asignada, entregada] = filasTabla(el());

    expect(botonDe(pendiente, 'Asignar')).toBeTruthy();
    expect(botonDe(asignada, 'En ruta')).toBeTruthy();
    expect(entregada.textContent).toContain('Ver ›');
    expect(entregada.textContent).not.toContain('En ruta');
  });

  it('CA-6-31 la pestaña Agencia pide solo las entregas por agencia y ofrece "Registrar envío"', async () => {
    const { el, ctrl, estabilizar } = await abrirRuta(
      'entregas',
      ENTREGAS_ROUTES,
      '/entregas?modalidad=AGENCIA',
    );
    ctrl.expectOne((r) => r.url === REPARTIDORES).flush(respuesta([REPARTIDOR]));
    const p = ctrl.expectOne((r) => r.url === LISTA);
    expect(p.request.params.get('modalidad')).toBe('AGENCIA');
    p.flush(
      respuesta(
        pagina(
          [
            resumen('21', {
              modalidad: 'AGENCIA',
              estado: 'PENDIENTE_ENVIO',
              siguienteAccion: 'ENVIADA_AGENCIA',
            }),
          ],
          {
            PENDIENTE_ENVIO: 1,
          },
        ),
      ),
    );
    await estabilizar();

    expect(botonDe(filasTabla(el())[0], 'Registrar envío')).toBeTruthy();
    expect(el().textContent).toContain('Pendiente de envío');
  });

  it('CA-6-32 los filtros de estado, fecha, repartidor y código viajan en la consulta y quedan en la URL', async () => {
    const { ctrl, estabilizar } = await abrirRuta(
      'entregas',
      ENTREGAS_ROUTES,
      '/entregas?estado=ASIGNADA&fecha=2026-10-19&repartidorId=101&q=FS-000012',
    );
    ctrl.expectOne((r) => r.url === REPARTIDORES).flush(respuesta([REPARTIDOR]));
    const p = ctrl.expectOne((r) => r.url === LISTA);

    expect(p.request.params.get('estado')).toBe('ASIGNADA');
    expect(p.request.params.get('fecha')).toBe('2026-10-19');
    expect(p.request.params.get('repartidorId')).toBe('101');
    expect(p.request.params.get('codigo')).toBe('FS-000012');
    p.flush(respuesta(pagina([LOCALES[1]], CONTADORES)));
    await estabilizar();
  });

  it('CA-6-32 cambiar de pestaña vuelve a consultar sin el estado elegido', async () => {
    const { el, estabilizar, lista, cargar } = await abrir('/entregas?estado=ASIGNADA');
    await cargar();
    (el().querySelectorAll('[role="tab"]')[1] as HTMLButtonElement).click();
    await estabilizar();

    const p = lista();
    expect(p.request.params.get('modalidad')).toBe('AGENCIA');
    expect(p.request.params.has('estado')).toBe(false);
    p.flush(respuesta(pagina([], {})));
    await estabilizar();
    expect(el().textContent).toContain('Sin entregas');
  });

  it('CA-6-21 el botón "En ruta" lee la entrega para tomar su versión y la avanza', async () => {
    const { el, estabilizar, cargar, esperar, lista } = await abrir();
    await cargar();

    botonDe(filasTabla(el())[1], 'En ruta').click();
    const detalle = await esperar((r) => r.url === `${LISTA}/12` && r.method === 'GET');
    detalle.flush(
      respuesta(entrega('12', { estado: 'ASIGNADA', version: 3, accionesPermitidas: ['EN_RUTA'] })),
    );
    const patch = await esperar((r) => r.url === `${LISTA}/12/en-ruta` && r.method === 'PATCH');
    expect((patch as unknown as { request: { body: unknown } }).request.body).toEqual({
      version: 3,
    });
    patch.flush(respuesta(entrega('12', { estado: 'EN_RUTA', version: 4 })));
    await estabilizar();

    // y la lista se vuelve a leer
    lista().flush(respuesta(pagina(LOCALES, CONTADORES)));
  });

  it('CA-6-20 el botón "Asignar" abre el diálogo y manda repartidor, fecha y versión', async () => {
    const { el, estabilizar, cargar, esperar, botonPanel, panel } = await abrir();
    await cargar();

    botonDe(filasTabla(el())[0], 'Asignar').click();
    const detalle = await esperar((r) => r.url === `${LISTA}/11` && r.method === 'GET');
    detalle.flush(respuesta(entrega('11', { version: 0 })));
    const repartidores = await esperar((r) => r.url === REPARTIDORES);
    repartidores.flush(respuesta([REPARTIDOR]));
    await estabilizar();

    const select = panel().querySelector('#repartidor-asignar') as HTMLSelectElement;
    select.value = '101';
    select.dispatchEvent(new Event('change'));
    const fecha = panel().querySelector('#fecha-asignar') as HTMLInputElement;
    fecha.value = '2099-01-01';
    fecha.dispatchEvent(new Event('input'));
    await estabilizar();
    botonPanel('Guardar').click();

    const patch = await esperar((r) => r.url === `${LISTA}/11/asignar` && r.method === 'PATCH');
    expect((patch as unknown as { request: { body: unknown } }).request.body).toEqual({
      repartidorId: 101,
      fechaProgramada: '2099-01-01',
      version: 0,
    });
    patch.flush(respuesta(entrega('11', { estado: 'ASIGNADA', version: 1 })));
    (await esperar((r) => r.url === LISTA && r.method === 'GET')).flush(
      respuesta(pagina(LOCALES, CONTADORES)),
    );
    await estabilizar();
  });

  it('CA-6-24 si la entrega cambió (409), se vuelve a leer la lista', async () => {
    const { el, estabilizar, cargar, esperar, lista } = await abrir();
    await cargar();

    botonDe(filasTabla(el())[1], 'En ruta').click();
    (await esperar((r) => r.url === `${LISTA}/12` && r.method === 'GET')).flush(
      respuesta(entrega('12', { estado: 'ASIGNADA', version: 1 })),
    );
    (await esperar((r) => r.url === `${LISTA}/12/en-ruta`)).flush(
      {
        success: false,
        mensaje: 'La entrega cambió; recarga la página',
        codigo: 'CONFLICTO_VERSION',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await estabilizar();

    lista().flush(respuesta(pagina(LOCALES, CONTADORES)));
  });
});
