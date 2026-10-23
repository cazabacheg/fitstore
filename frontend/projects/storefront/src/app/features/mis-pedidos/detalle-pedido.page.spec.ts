import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { orderApi } from 'shared';
import { vi } from 'vitest';
import { montar } from '../../testing';
import { PROVEEDORES, errorApi, http, ok } from '../carrito/pruebas-carrito';
import { DetallePedidoPage } from './detalle-pedido.page';
import { MisPedidosPage } from './mis-pedidos.page';

const RUTA = '/api/v1/pedidos';

function paso(
  estadoNuevo: orderApi.EstadoPedido,
  fecha: string,
  extra: Partial<orderApi.HistorialEntrada> = {},
): orderApi.HistorialEntrada {
  return { fecha, tipo: 'CAMBIO_ESTADO', estadoNuevo, actorTipo: 'SISTEMA', ...extra };
}

function pedido(extra: Partial<orderApi.PedidoDetalle> = {}): orderApi.PedidoDetalle {
  return {
    codigo: 'FS-000124',
    estado: 'PENDIENTE_PAGO',
    modalidad: 'DELIVERY_LOCAL',
    creadoEn: '2026-10-20T15:00:00Z',
    expiraEn: null,
    items: [
      {
        varianteId: 101,
        sku: 'LEG-NEG-M',
        producto: 'Legging Pro',
        talla: 'M',
        color: 'Negro',
        imagenUrl: null,
        precioUnitario: 59.9,
        cantidad: 2,
        descuentoPorcentaje: 0,
        descuentoMonto: 0,
        total: 119.8,
      },
    ],
    subtotal: 119.8,
    descuentoTotal: 0,
    costoEnvio: 10,
    total: 129.8,
    montoReembolsado: 0,
    estadoReembolso: 'NINGUNO',
    destinatario: { nombres: 'Ana', apellidos: 'Quispe', dni: '12345678', telefono: '987654321', email: 'a@b.pe' },
    direccion: {
      ubigeo: '150122',
      departamento: 'Lima',
      provincia: 'Lima',
      distrito: 'Miraflores',
      direccion: 'Av. Larco 345',
    },
    envio: { modalidad: 'DELIVERY_LOCAL', costo: 10 },
    historial: [paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z')],
    puedeCancelar: true,
    puedePagar: false,
    ...extra,
  };
}

async function abrir(datos: orderApi.PedidoDetalle | null, estadoHttp = 200) {
  const m = await montar(DetallePedidoPage, {}, [
    ...PROVEEDORES,
    { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ codigo: 'FS-000124' }) } } },
  ]);
  const peticion = http().expectOne(`${RUTA}/FS-000124`);
  if (datos) peticion.flush(ok(datos));
  else peticion.flush(errorApi('NO_ENCONTRADO', 'No existe'), { status: estadoHttp, statusText: 'Error' });
  await m.estabilizar();
  const boton = (texto: string) =>
    [...m.el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(texto));
  return { ...m, boton };
}

describe('Detalle de mi pedido (P-4-04)', () => {
  afterEach(() => {
    vi.useRealTimers();
    http().verify();
  });

  it('CA-4-34 la línea de tiempo muestra cada paso con fecha y hora, y las etapas futuras atenuadas con su descripción', async () => {
    const p = await abrir(
      pedido({
        estado: 'EN_PREPARACION',
        puedeCancelar: false,
        historial: [
          paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'),
          paso('PAGADO', '2026-10-20T15:05:00Z'),
          paso('EN_PREPARACION', '2026-10-20T16:30:00Z'),
        ],
      }),
    );

    const etapas = [...p.el.querySelectorAll<HTMLElement>('.fs-etapa')];
    expect(etapas.map((e) => e.querySelector('.fs-etapa__nombre')?.textContent)).toEqual([
      'Pedido recibido',
      'Pago confirmado',
      'En preparación',
      'Enviado',
      'Entregado',
    ]);
    expect(etapas[0].querySelector('.fs-etapa__fecha')?.textContent).toContain('20/10/2026 10:00');
    expect(etapas[0].textContent).toContain('Apartamos tus prendas mientras completas el pago.');
    expect(etapas[2].classList).toContain('fs-etapa--actual');
    expect(etapas[3].classList).toContain('fs-etapa--futura');
    expect(etapas[3].style.opacity).toBe('0.5');
    expect(etapas[3].textContent).toContain('Tu pedido va en camino con nuestro repartidor.');
    expect(etapas[3].querySelector('.fs-etapa__fecha')).toBeNull();
  });

  it('CA-4-34 un pedido por agencia explica en qué agencia quedó y con qué código seguirlo; los intentos fallidos salen como avisos', async () => {
    const p = await abrir(
      pedido({
        estado: 'ENVIADO',
        modalidad: 'AGENCIA',
        puedeCancelar: false,
        envio: { modalidad: 'AGENCIA', costo: 18, agenciaNombre: 'Shalom', codigoSeguimiento: 'SH-123' },
        historial: [
          paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'),
          paso('PAGADO', '2026-10-20T15:05:00Z'),
          paso('EN_PREPARACION', '2026-10-20T16:30:00Z'),
          paso('ENVIADO', '2026-10-21T10:00:00Z'),
          { fecha: '2026-10-22T10:00:00Z', tipo: 'INTENTO_FALLIDO', actorTipo: 'SISTEMA', comentario: 'Intento fallido (1): Cliente ausente' },
        ],
      }),
    );

    expect(p.el.textContent).toContain('Lo dejamos en la agencia Shalom; síguelo con el código SH-123.');
    expect(p.el.querySelector('.fs-aviso-historial')?.textContent).toContain('Cliente ausente');
  });

  it('CA-4-85 si un pago fue rechazado y el siguiente se aprobó, el pago confirmado también sale en los avisos, después del rechazo', async () => {
    const p = await abrir(
      pedido({
        estado: 'PAGADO',
        puedeCancelar: false,
        historial: [
          paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'),
          { fecha: '2026-10-20T15:02:00Z', tipo: 'PAGO_RECHAZADO', actorTipo: 'SISTEMA', comentario: 'Pago rechazado: Fondos insuficientes' },
          paso('PAGADO', '2026-10-20T15:05:00Z', { comentario: 'Pago confirmado' }),
        ],
      }),
    );

    const avisos = [...p.el.querySelectorAll<HTMLElement>('.fs-aviso-historial')];
    expect(avisos.map((a) => a.textContent)).toEqual([
      expect.stringContaining('Fondos insuficientes'),
      expect.stringContaining('Pago confirmado'),
    ]);
    expect(avisos[1].classList.contains('fs-aviso-historial--exito')).toBe(true);
    expect(avisos[0].classList.contains('fs-aviso-historial--exito')).toBe(false);
  });

  it('CA-4-85 sin avisos previos, el pago confirmado no se repite como aviso (ya está en la línea de tiempo)', async () => {
    const p = await abrir(
      pedido({
        estado: 'PAGADO',
        puedeCancelar: false,
        historial: [paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'), paso('PAGADO', '2026-10-20T15:05:00Z', { comentario: 'Pago confirmado' })],
      }),
    );

    expect(p.el.querySelectorAll('.fs-aviso-historial').length).toBe(0);
  });

  it('CA-4-34 un recojo termina en "Listo para recoger" y "Entregado" con el texto del recojo', async () => {
    const p = await abrir(
      pedido({
        estado: 'LISTO_RECOJO',
        modalidad: 'RECOJO',
        direccion: null,
        envio: { modalidad: 'RECOJO', costo: 0, tiendaDireccion: 'Jr. Gamarra 500' },
        historial: [paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'), paso('PAGADO', '2026-10-20T15:05:00Z'), paso('EN_PREPARACION', '2026-10-20T16:00:00Z'), paso('LISTO_RECOJO', '2026-10-21T09:00:00Z')],
      }),
    );

    expect(p.el.textContent).toContain('Ya puedes recogerlo en la tienda; lleva tu DNI.');
    expect(p.el.textContent).toContain('Recogiste tu pedido en la tienda.');
    expect(p.el.textContent).toContain('Recojo en tienda: Jr. Gamarra 500');
  });

  it('CA-4-34 un pedido devuelto termina en "Devuelto a la tienda" con el motivo', async () => {
    const p = await abrir(
      pedido({
        estado: 'DEVUELTO',
        puedeCancelar: false,
        historial: [paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'), paso('DEVUELTO', '2026-10-25T15:00:00Z', { comentario: 'Rechazado en agencia' })],
      }),
    );

    const etapas = [...p.el.querySelectorAll<HTMLElement>('.fs-etapa__nombre')].map((e) => e.textContent);
    expect(etapas.at(-1)).toBe('Devuelto a la tienda');
    expect(p.el.textContent).toContain('regresó a la tienda: Rechazado en agencia');
    expect(p.el.querySelectorAll('.fs-etapa--futura')).toHaveLength(0);
  });

  it('CA-4-35 un pedido pendiente de pago muestra cuánto tiempo queda y "Pagar ahora"', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] });
    vi.setSystemTime(new Date('2026-10-20T15:00:00Z'));
    const p = await abrir(pedido({ puedePagar: true, expiraEn: '2026-10-20T15:24:10Z' }));

    expect(p.el.querySelector('.fs-cuenta-regresiva')?.textContent).toContain('Te quedan 24:10 para pagar');
    const pagar = [...p.el.querySelectorAll<HTMLAnchorElement>('a')].find((a) => a.textContent?.includes('Pagar ahora'))!;
    expect(pagar.getAttribute('href')).toBe('/pago/FS-000124');

    vi.advanceTimersByTime(10_000);
    await p.estabilizar();
    expect(p.el.querySelector('.fs-cuenta-regresiva')?.textContent).toContain('Te quedan 24:00 para pagar');
  });

  it('CA-4-35 sin plazo por pagar no aparece "Pagar ahora"', async () => {
    const p = await abrir(pedido({ estado: 'PAGADO', puedePagar: false }));

    expect(p.el.textContent).not.toContain('Pagar ahora');
  });

  it('CA-4-36 con boleta ofrece "Descargar boleta" y la pide a la API', async () => {
    const crear = vi.fn(() => 'blob:boleta');
    const revocar = vi.fn();
    Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar });
    const p = await abrir(pedido({ estado: 'PAGADO', boletaUrl: '/api/v1/pagos/FS-000124/boleta' }));

    p.boton('Descargar boleta')!.click();
    const descarga = http().expectOne('/api/v1/pagos/FS-000124/boleta');
    expect(descarga.request.responseType).toBe('blob');
    descarga.flush(new Blob(['pdf']));
    expect(crear).toHaveBeenCalled();
  });

  it('CA-4-36 sin boleta no hay botón de descarga', async () => {
    const p = await abrir(pedido({ estado: 'PAGADO' }));

    expect(p.boton('Descargar boleta')).toBeUndefined();
  });

  it('CA-4-37 un pedido ajeno o inexistente muestra "No encontramos ese pedido"', async () => {
    const p = await abrir(null, 404);

    expect(p.el.textContent).toContain('No encontramos ese pedido');
  });

  it('CA-4-38 cancelar un pedido pendiente pide confirmación y deja el pedido cancelado', async () => {
    const p = await abrir(pedido());

    p.boton('Cancelar pedido')!.click();
    await p.estabilizar();
    http().expectNone(`${RUTA}/FS-000124/cancelar`);
    [...p.el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')]
      .find((b) => b.textContent?.includes('Sí, cancelar pedido'))!
      .click();
    const peticion = http().expectOne(`${RUTA}/FS-000124/cancelar`);
    expect(peticion.request.method).toBe('PATCH');
    peticion.flush(
      ok(
        pedido({
          estado: 'CANCELADO',
          puedeCancelar: false,
          historial: [paso('PENDIENTE_PAGO', '2026-10-20T15:00:00Z'), paso('CANCELADO', '2026-10-20T15:10:00Z', { comentario: 'Cancelado por el cliente' })],
        }),
      ),
    );
    await p.estabilizar();

    expect(p.el.textContent).toContain('Cancelado');
    expect(p.el.textContent).toContain('lo cancelaste tú');
    expect(p.boton('Cancelar pedido')).toBeUndefined();
    expect(p.el.textContent).not.toContain('Te devolveremos');
  });

  it('CA-4-38 al cancelar un pedido pagado dice "Te devolveremos S/ X al mismo medio de pago"', async () => {
    const p = await abrir(pedido({ estado: 'PAGADO' }));

    p.boton('Cancelar pedido')!.click();
    await p.estabilizar();
    expect(p.el.querySelector('[role="dialog"]')?.textContent).toContain('Te devolveremos S/ 129.80');
    [...p.el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')]
      .find((b) => b.textContent?.includes('Sí, cancelar pedido'))!
      .click();
    http()
      .expectOne(`${RUTA}/FS-000124/cancelar`)
      .flush(ok(pedido({ estado: 'CANCELADO', puedeCancelar: false })));
    await p.estabilizar();

    expect(p.el.querySelector('.fs-banner-exito')?.textContent).toContain('Te devolveremos S/ 129.80 al mismo medio de pago');
  });

  it('CA-4-39 desde EN_PREPARACION no hay botón de cancelar', async () => {
    const p = await abrir(pedido({ estado: 'EN_PREPARACION', puedeCancelar: false }));

    expect(p.boton('Cancelar pedido')).toBeUndefined();
  });
});

describe('Mis pedidos (P-4-03)', () => {
  afterEach(() => http().verify());

  const resumen = (codigo: string): orderApi.PedidoResumen => ({
    codigo,
    creadoEn: '2026-10-20T15:00:00Z',
    estado: 'PAGADO',
    total: 129.8,
    modalidad: 'DELIVERY_LOCAL',
    cantidadProductos: 3,
    miniaturaUrl: 'http://media/1.jpg',
  });

  it('CA-4-33 lista mis pedidos con código, fecha, estado, total y miniatura, y pagina de 10 en 10', async () => {
    const m = await montar(MisPedidosPage, {}, PROVEEDORES);
    const primera = http().expectOne((r) => r.url === RUTA);
    expect(primera.request.params.get('page')).toBe('0');
    expect(primera.request.params.get('size')).toBe('10');
    primera.flush(
      ok({ contenido: [resumen('FS-000125'), resumen('FS-000124')], pagina: 0, tamano: 10, totalElementos: 12, totalPaginas: 2 }),
    );
    await m.estabilizar();

    const filas = [...m.el.querySelectorAll<HTMLElement>('.fs-tarjeta-pedido')];
    expect(filas).toHaveLength(2);
    expect(filas[0].textContent).toContain('FS-000125');
    expect(filas[0].textContent).toContain('20/10/2026');
    expect(filas[0].textContent).toContain('Pagado');
    expect(filas[0].textContent).toContain('S/ 129.80');
    expect(filas[0].querySelector('img')?.getAttribute('src')).toBe('http://media/1.jpg');
    expect(filas[0].querySelector('a')?.getAttribute('href')).toBe('/cuenta/pedidos/FS-000125');

    m.el.querySelector<HTMLButtonElement>('button[data-pagina="1"]')!.click();
    const segunda = http().expectOne((r) => r.url === RUTA);
    expect(segunda.request.params.get('page')).toBe('1');
    segunda.flush(ok({ contenido: [resumen('FS-000100')], pagina: 1, tamano: 10, totalElementos: 12, totalPaginas: 2 }));
  });

  it('CA-4-33 sin pedidos muestra "Aún no tienes pedidos" con un enlace a la tienda', async () => {
    const m = await montar(MisPedidosPage, {}, PROVEEDORES);
    http().expectOne((r) => r.url === RUTA).flush(ok({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 }));
    await m.estabilizar();

    expect(m.el.textContent).toContain('Aún no tienes pedidos');
    expect(TestBed.inject(ActivatedRoute)).toBeTruthy();
  });

  it('CA-4-33 si la API falla ofrece reintentar', async () => {
    const m = await montar(MisPedidosPage, {}, PROVEEDORES);
    http().expectOne((r) => r.url === RUTA).flush(errorApi('ERROR_INTERNO', 'Falló'), { status: 500, statusText: 'Error' });
    await m.estabilizar();

    expect(m.el.textContent).toContain('No pudimos cargar tus pedidos');
  });
});
