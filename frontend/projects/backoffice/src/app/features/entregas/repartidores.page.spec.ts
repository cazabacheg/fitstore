import { REPARTIDORES_ROUTES } from './entregas.routes';
import { abrirRuta, botonDe, filasTabla, REPARTIDOR, respuesta } from './datos-prueba';
import type { deliveryApi } from 'shared';

vi.setConfig({ testTimeout: 30000 });

const URL = '/api/v1/admin/repartidores';
const CON_ENTREGAS: deliveryApi.Repartidor = {
  ...REPARTIDOR,
  id: 102,
  nombres: 'Rosa Quispe',
  tieneEntregas: true,
};
const INACTIVO: deliveryApi.Repartidor = {
  ...REPARTIDOR,
  id: 103,
  nombres: 'Luis Castro',
  vehiculo: 'BICICLETA',
  placa: null,
  activo: false,
};

async function abrir() {
  const montaje = await abrirRuta('repartidores', REPARTIDORES_ROUTES, '/repartidores');
  const lista = () => montaje.ctrl.expectOne((r) => r.url === URL && r.method === 'GET');
  const cargar = async (filas = [REPARTIDOR, CON_ENTREGAS, INACTIVO]) => {
    lista().flush(respuesta(filas));
    await montaje.estabilizar();
  };
  const fila = (nombre: string) =>
    filasTabla(montaje.el()).find((f) => f.textContent?.includes(nombre))!;
  return { ...montaje, lista, cargar, fila };
}

describe('RepartidoresPage (P-6-04)', () => {
  it('CA-6-16 lista los repartidores con celular, vehículo con placa y estado', async () => {
    const { cargar, fila } = await abrir();
    await cargar();

    expect(fila('Juan Pérez').textContent).toContain('987654321');
    expect(fila('Juan Pérez').textContent).toContain('Moto · ABC-123');
    expect(fila('Luis Castro').textContent).toContain('Bicicleta');
    expect(fila('Luis Castro').textContent).toContain('Inactivo');
  });

  it('CA-6-16 registrar valida el celular y la placa antes de enviar y luego crea al repartidor', async () => {
    const { el, estabilizar, ctrl, cargar, panel, botonPanel, esperar } = await abrir();
    await cargar();

    botonDe(el(), 'Registrar repartidor').click();
    await estabilizar();
    const campos = () => panel().querySelectorAll<HTMLInputElement>('input');
    const escribir = async (indice: number, valor: string) => {
      campos()[indice].value = valor;
      campos()[indice].dispatchEvent(new Event('input'));
      await estabilizar();
    };
    await escribir(0, 'Pedro Gómez');
    await escribir(1, '887654321');
    botonPanel('Registrar').click();
    await estabilizar();
    expect(panel().textContent).toContain('9 dígitos que empiece con 9');
    expect(panel().textContent).toContain('La placa es obligatoria para moto y auto');
    expect(ctrl.match((r) => r.url === URL && r.method === 'POST').length).toBe(0);

    await escribir(1, '987000111');
    await escribir(2, 'PST-001');
    botonPanel('Registrar').click();
    const post = await esperar((r) => r.url === URL && r.method === 'POST');
    expect((post as unknown as { request: { body: unknown } }).request.body).toEqual({
      nombres: 'Pedro Gómez',
      telefono: '987000111',
      vehiculo: 'MOTO',
      placa: 'PST-001',
    });
    post.flush(respuesta({ ...REPARTIDOR, id: 200, nombres: 'Pedro Gómez' }));
    (await esperar((r) => r.url === URL && r.method === 'GET')).flush(respuesta([REPARTIDOR]));
    await estabilizar();
  });

  it('CA-6-17 desactivar pide confirmación y deja al repartidor inactivo; uno inactivo ofrece "Activar"', async () => {
    const { estabilizar, cargar, fila, botonPanel, esperar } = await abrir();
    await cargar();
    expect(botonDe(fila('Luis Castro'), 'Activar')).toBeTruthy();

    botonDe(fila('Juan Pérez'), 'Desactivar').click();
    await estabilizar();
    botonPanel('Desactivar').click();
    const patch = await esperar((r) => r.url === `${URL}/101/desactivar` && r.method === 'PATCH');
    patch.flush(respuesta({ ...REPARTIDOR, activo: false }));
    (await esperar((r) => r.url === URL && r.method === 'GET')).flush(
      respuesta([{ ...REPARTIDOR, activo: false }, CON_ENTREGAS, INACTIVO]),
    );
    await estabilizar();
  });

  it('CA-6-18 "Eliminar" solo aparece en los repartidores sin entregas y los demás se desactivan', async () => {
    const { cargar, fila, estabilizar, botonPanel, esperar } = await abrir();
    await cargar();

    expect(botonDe(fila('Juan Pérez'), 'Eliminar')).toBeTruthy();
    expect(botonDe(fila('Rosa Quispe'), 'Eliminar')).toBeUndefined();
    expect(botonDe(fila('Rosa Quispe'), 'Desactivar')).toBeTruthy();

    botonDe(fila('Juan Pérez'), 'Eliminar').click();
    await estabilizar();
    botonPanel('Eliminar').click();
    const borrar = await esperar((r) => r.url === `${URL}/101` && r.method === 'DELETE');
    borrar.flush(respuesta(null));
    (await esperar((r) => r.url === URL && r.method === 'GET')).flush(
      respuesta([CON_ENTREGAS, INACTIVO]),
    );
    await estabilizar();
  });

  it('CA-6-18 si el servidor responde 409 al eliminar, se vuelve a leer la lista', async () => {
    const { cargar, fila, estabilizar, botonPanel, esperar } = await abrir();
    await cargar();

    botonDe(fila('Juan Pérez'), 'Eliminar').click();
    await estabilizar();
    botonPanel('Eliminar').click();
    (await esperar((r) => r.url === `${URL}/101` && r.method === 'DELETE')).flush(
      {
        success: false,
        mensaje: 'El repartidor tiene entregas; desactívalo en vez de eliminarlo',
        codigo: 'REPARTIDOR_CON_ENTREGAS',
      },
      { status: 409, statusText: 'Conflict' },
    );
    (await esperar((r) => r.url === URL && r.method === 'GET')).flush(
      respuesta([{ ...REPARTIDOR, tieneEntregas: true }, CON_ENTREGAS, INACTIVO]),
    );
    await estabilizar();
  });

  it('CA-6-16 sin repartidores muestra el estado vacío', async () => {
    const { el, cargar } = await abrir();
    await cargar([]);

    expect(el().textContent).toContain('Sin repartidores');
  });
});
