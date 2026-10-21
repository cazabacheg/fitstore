import { ENTREGAS_ROUTES } from './entregas.routes';
import { abrirRuta, botonDe, entrega, respuesta } from './datos-prueba';

vi.setConfig({ testTimeout: 30000 });

const URL = '/api/v1/admin/entregas';

const EN_RUTA = entrega('12', {
  estado: 'EN_RUTA',
  siguienteAccion: 'ENTREGADA',
  accionesPermitidas: ['ENTREGADA', 'FALLIDA'],
  repartidor: { id: 101, nombres: 'Juan Pérez', vehiculo: 'MOTO', placa: 'ABC-123' },
  repartidorNombre: 'Juan Pérez',
  fechaProgramada: '2026-10-19',
  intento: 1,
  version: 2,
  eventos: [
    {
      estado: 'PENDIENTE_ASIGNACION',
      observacion: null,
      actorNombre: null,
      intento: 0,
      fecha: '2026-10-19T15:00:00Z',
    },
    {
      estado: 'ASIGNADA',
      observacion: null,
      actorNombre: 'Olga Operadora',
      intento: 1,
      fecha: '2026-10-19T15:10:00Z',
    },
    {
      estado: 'EN_RUTA',
      observacion: null,
      actorNombre: 'Olga Operadora',
      intento: 1,
      fecha: '2026-10-19T16:00:00Z',
    },
  ],
});

async function abrir(detalle = EN_RUTA, id = '12') {
  const montaje = await abrirRuta('entregas', ENTREGAS_ROUTES, `/entregas/${id}`);
  montaje.ctrl
    .expectOne((r) => r.url === `${URL}/${id}` && r.method === 'GET')
    .flush(respuesta(detalle));
  await montaje.estabilizar();
  return montaje;
}

describe('DetalleEntregaPage (P-6-02)', () => {
  it('CA-6-21 muestra el destinatario, la dirección, los ítems y el seguimiento con quién hizo cada paso', async () => {
    const { el } = await abrir();
    const texto = el().textContent ?? '';

    expect(texto).toContain('FS-000012');
    expect(texto).toContain('Lucía Mendoza');
    expect(texto).toContain('45678912');
    expect(texto).toContain('Av. Larco 345 dpto 201');
    expect(texto).toContain('Frente al parque Kennedy');
    expect(texto).toContain('Legging Pro Compresión M Negro × 2');
    expect(texto).toContain('Juan Pérez');
    const pasos = [...el().querySelectorAll('[data-testid="evento"]')];
    expect(pasos.length).toBe(3);
    expect(pasos[0].textContent).toContain('Sistema');
    expect(pasos[2].textContent).toContain('Olga Operadora');
  });

  it('CA-6-21 las acciones salen de accionesPermitidas: Entregada y No se pudo entregar, y nunca la devolución a tienda', async () => {
    const { el } = await abrir();
    const botones = [
      ...el().querySelectorAll<HTMLButtonElement>('section[aria-label="Acciones"] button'),
    ].map((b) => b.textContent?.trim());

    expect(botones).toEqual(['Entregada', 'No se pudo entregar']);
    expect(el().textContent).not.toContain('Devuelto a tienda');
  });

  it('CA-6-21 "Entregada" avanza la entrega con su versión y muestra el nuevo estado sin acciones', async () => {
    const { el, estabilizar, ctrl } = await abrir();

    botonDe(el(), 'Entregada').click();
    await estabilizar();
    const patch = ctrl.expectOne((r) => r.url === `${URL}/12/entregada`);
    expect(patch.request.body).toEqual({ version: 2 });
    patch.flush(
      respuesta({
        ...EN_RUTA,
        estado: 'ENTREGADA',
        accionesPermitidas: [],
        siguienteAccion: null,
        version: 3,
      }),
    );
    await estabilizar();

    expect(el().textContent).toContain('Entregada');
    expect(el().querySelector('section[aria-label="Acciones"]')).toBeNull();
  });

  it('CA-6-22 "No se pudo entregar" exige la observación y la manda con la versión', async () => {
    const { el, estabilizar, ctrl, panel, botonPanel, esperar, hasta } = await abrir();

    botonDe(el(), 'No se pudo entregar').click();
    await estabilizar();
    botonPanel('Marcar como fallida').click();
    await estabilizar();
    expect(panel().textContent).toContain('Cuéntanos por qué no se pudo entregar');
    expect(ctrl.match((r) => r.url.endsWith('/fallida')).length).toBe(0);

    const campo = panel().querySelector('input') as HTMLInputElement;
    campo.value = 'No había nadie';
    campo.dispatchEvent(new Event('input'));
    await estabilizar();
    botonPanel('Marcar como fallida').click();
    const patch = await esperar((r) => r.url === `${URL}/12/fallida`);
    expect((patch as unknown as { request: { body: unknown } }).request.body).toEqual({
      observacion: 'No había nadie',
      version: 2,
    });
    patch.flush(
      respuesta({
        ...EN_RUTA,
        estado: 'FALLIDA',
        accionesPermitidas: ['REPROGRAMAR'],
        siguienteAccion: 'REPROGRAMAR',
        version: 3,
      }),
    );
    await hasta(() => !!el().textContent?.includes('Fallida'));

    expect(el().textContent).toContain('Fallida');
    expect(botonDe(el(), 'Reprogramar')).toBeTruthy();
  });

  it('CA-6-24 si otro operador la cambió (409) se vuelve a leer la entrega', async () => {
    const { el, estabilizar, ctrl } = await abrir();

    botonDe(el(), 'Entregada').click();
    await estabilizar();
    ctrl
      .expectOne((r) => r.url === `${URL}/12/entregada`)
      .flush(
        {
          success: false,
          mensaje: 'La entrega cambió; recarga la página',
          codigo: 'CONFLICTO_VERSION',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await estabilizar();

    ctrl
      .expectOne((r) => r.url === `${URL}/12` && r.method === 'GET')
      .flush(respuesta({ ...EN_RUTA, version: 5 }));
  });

  it('CA-6-26 una entrega por agencia muestra el código, la clave y el enlace de rastreo, y "Registrar envío" pide el código', async () => {
    const pendiente = entrega('21', {
      modalidad: 'AGENCIA',
      estado: 'PENDIENTE_ENVIO',
      siguienteAccion: 'ENVIADA_AGENCIA',
      accionesPermitidas: ['ENVIADA_AGENCIA'],
      agenciaId: 1,
      agenciaNombre: 'Shalom',
    });
    const { el, estabilizar, ctrl, panel, botonPanel, esperar, hasta } = await abrir(
      pendiente,
      '21',
    );

    botonDe(el(), 'Registrar envío').click();
    await estabilizar();
    const agencias = await esperar((r) => r.url === '/api/v1/agencias');
    agencias.flush(
      respuesta([{ id: 1, nombre: 'Shalom', urlSeguimiento: null, activa: true, orden: 1 }]),
    );
    await estabilizar();
    botonPanel('Registrar envío').click();
    await estabilizar();
    expect(panel().textContent).toContain('Ingresa el código de seguimiento');
    expect(ctrl.match((r) => r.url.endsWith('/enviada-agencia')).length).toBe(0);

    const campos = panel().querySelectorAll<HTMLInputElement>('input');
    campos[0].value = 'SH-5566';
    campos[0].dispatchEvent(new Event('input'));
    campos[1].value = '4821';
    campos[1].dispatchEvent(new Event('input'));
    await estabilizar();
    botonPanel('Registrar envío').click();
    const patch = await esperar((r) => r.url === `${URL}/21/enviada-agencia`);
    expect((patch as unknown as { request: { body: unknown } }).request.body).toEqual({
      agenciaId: null,
      codigoSeguimiento: 'SH-5566',
      claveRecojo: '4821',
      version: 0,
    });
    patch.flush(
      respuesta({
        ...pendiente,
        estado: 'ENVIADO_AGENCIA',
        codigoSeguimiento: 'SH-5566',
        claveRecojo: '4821',
        urlSeguimiento: 'https://rastreo.ejemplo.pe/shalom?codigo=SH-5566',
        accionesPermitidas: ['DISPONIBLE_AGENCIA'],
        siguienteAccion: 'DISPONIBLE_AGENCIA',
        version: 1,
      }),
    );
    await hasta(() => !!el().textContent?.includes('SH-5566'));

    expect(el().textContent).toContain('SH-5566');
    expect(el().textContent).toContain('4821');
    const enlace = el().querySelector('a[href^="https://rastreo.ejemplo.pe"]') as HTMLAnchorElement;
    expect(enlace.getAttribute('href')).toBe('https://rastreo.ejemplo.pe/shalom?codigo=SH-5566');
  });

  it('CA-6-31 una entrega que no existe muestra "No encontramos esta entrega"', async () => {
    const { el, estabilizar, ctrl } = await abrirRuta('entregas', ENTREGAS_ROUTES, '/entregas/999');
    ctrl
      .expectOne((r) => r.url === `${URL}/999`)
      .flush(
        { success: false, mensaje: 'No encontramos esa entrega', codigo: 'NO_ENCONTRADO' },
        { status: 404, statusText: 'x' },
      );
    await estabilizar();

    expect(el().querySelector('[data-testid="no-encontrada"]')).not.toBeNull();
  });
});
