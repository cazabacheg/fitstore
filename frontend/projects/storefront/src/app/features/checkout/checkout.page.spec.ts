import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CarritoContador, authApi, orderApi } from 'shared';
import { vi } from 'vitest';
import { montar } from '../../testing';
import { PROVEEDORES, carrito, errorApi, http, linea, ok } from '../carrito/pruebas-carrito';
import { DialogoDireccionService } from '../cuenta/direcciones/dialogo-direccion.service';
import { CheckoutPage } from './checkout.page';

const dir = (id: number, alias: string, ubigeo = '150122', predeterminada = false): authApi.Direccion => ({
  id,
  alias,
  ubigeo,
  direccion: `Jr. ${alias} ${id}00`,
  referencia: null,
  telefonoContacto: null,
  predeterminada,
});

const resumenDe = (total: number, extra: Partial<orderApi.ResumenCheckout> = {}): orderApi.ResumenCheckout => ({
  items: [
    {
      varianteId: 101,
      sku: 'LEG-M',
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
  costoEnvio: total - 119.8,
  total,
  envio: { modalidad: 'DELIVERY_LOCAL', costo: total - 119.8, plazoDiasMin: 1, plazoDiasMax: 2 },
  expiracionMinutos: 30,
  ...extra,
});

/** Carrito del cliente: lo que el checkout muestra mientras el servidor no tiene un resumen de envío para devolver. */
const CARRITO_BASE = carrito([linea(101, 'Legging Pro', 2, { descuentoPorcentaje: 10, descuentoMonto: 12, total: 107.8 })]);

const OPCIONES = [
  { modalidad: 'DELIVERY_LOCAL', costo: 10, plazoDiasMin: 1, plazoDiasMax: 2 },
  { modalidad: 'AGENCIA', costo: 18, plazoDiasMin: 2, plazoDiasMax: 3 },
];

const configuracion = (modalidades: string[] = ['DELIVERY_LOCAL', 'AGENCIA', 'RECOJO']) => ({
  contenido: {
    horario: 'Lun–Sáb 9:00–19:00',
    tiendaFisica: { direccion: 'Jr. Gamarra 500', ubigeo: '150115', ubigeoNombre: 'La Victoria, Lima, Lima' },
  },
  parametros: { modalidadesEntrega: modalidades, expiracionPedidoMinutos: 30, diasReembolsoPostventa: 30, metodosPago: ['TARJETA'], moneda: 'PEN' },
});

const abrirDialogo = vi.fn<() => Promise<authApi.Direccion | undefined>>();

async function iniciar(direcciones: authApi.Direccion[], opciones = OPCIONES, total = 129.8, modalidades?: string[]) {
  const m = await montar(CheckoutPage, {}, [...PROVEEDORES, { provide: DialogoDireccionService, useValue: { abrir: abrirDialogo } }]);
  http().expectOne('/api/v1/settings/public').flush(ok(configuracion(modalidades)));
  http().expectOne('/api/v1/carrito').flush(ok(CARRITO_BASE));
  http().expectOne('/api/v1/clientes/me/direcciones').flush(ok(direcciones));
  const principal = direcciones.find((d) => d.predeterminada) ?? direcciones[0];
  if (principal) {
    http().expectOne(`/api/v1/envios/opciones?ubigeo=${principal.ubigeo}`).flush(ok(opciones));
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(total)));
  } else {
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(119.8, { costoEnvio: 0, total: 119.8, envio: { modalidad: 'RECOJO', costo: 0, tiendaDireccion: 'Jr. Gamarra 500' } })));
  }
  await m.estabilizar();
  const boton = (texto: string) =>
    [...m.el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(texto))!;
  const radios = (nombre: string) => [...m.el.querySelectorAll<HTMLInputElement>(`input[name="${nombre}"]`)];
  return { ...m, boton, radios };
}

describe('Checkout (P-4-02)', () => {
  afterEach(() => {
    abrirDialogo.mockReset();
    http().verify();
  });

  it('CA-4-13 muestra mis direcciones con la predeterminada elegida y las modalidades con tarifa en su distrito', async () => {
    const p = await iniciar([dir(1, 'Casa'), dir(2, 'Trabajo', '150101', true)]);

    expect(p.radios('direccion').map((r) => r.checked)).toEqual([false, true]);
    const modalidades = p.radios('modalidad');
    expect(modalidades).toHaveLength(3);
    const texto = p.el.querySelector('[aria-labelledby="sec-entrega"]')?.textContent ?? '';
    expect(texto).toContain('Delivery a domicilio');
    expect(texto).toContain('S/ 10.00');
    expect(texto).toContain('1 a 2 días');
    expect(texto).toContain('Envío por agencia');
    expect(texto).toContain('Recojo en tienda');
    expect(modalidades[0].checked).toBe(true);
  });

  it('CA-4-13 recojo en tienda muestra la dirección y el horario de la tienda, sin costo', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[2].click();
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(119.8, { costoEnvio: 0, total: 119.8, envio: { modalidad: 'RECOJO', costo: 0 } })));
    await p.estabilizar();

    expect(p.el.querySelector('.fs-tienda')?.textContent).toContain('Jr. Gamarra 500, La Victoria, Lima, Lima · Lun–Sáb 9:00–19:00');
    expect(p.el.querySelector('[aria-labelledby="sec-entrega"]')?.textContent).toContain('Sin costo');
  });

  it('CA-4-13 solo ofrece las modalidades que la tienda habilitó', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)], OPCIONES, 129.8, ['DELIVERY_LOCAL', 'RECOJO']);

    expect(p.el.querySelector('[aria-labelledby="sec-entrega"]')?.textContent).not.toContain('Envío por agencia');
    expect(p.radios('modalidad')).toHaveLength(2);
  });

  it('CA-4-14 fuera de Lima y Callao el distrito no trae delivery local y no se ofrece', async () => {
    const m = await montar(CheckoutPage, {}, [...PROVEEDORES, { provide: DialogoDireccionService, useValue: { abrir: abrirDialogo } }]);
    http().expectOne('/api/v1/settings/public').flush(ok(configuracion()));
    http().expectOne('/api/v1/carrito').flush(ok(CARRITO_BASE));
    http().expectOne('/api/v1/clientes/me/direcciones').flush(ok([dir(1, 'Casa', '040101', true)]));
    http().expectOne('/api/v1/envios/opciones?ubigeo=040101').flush(ok([OPCIONES[1]]));
    // Con solo agencia disponible queda elegida y hay que escoger la agencia antes de pedir el resumen
    http().expectOne('/api/v1/agencias').flush(ok([{ id: 1, nombre: 'Shalom', activa: true, orden: 1 }]));
    await m.estabilizar();

    const texto = m.el.querySelector('[aria-labelledby="sec-entrega"]')?.textContent ?? '';
    expect(texto).not.toContain('Delivery a domicilio');
    expect(texto).toContain('Envío por agencia');
    expect(texto).toContain('Recojo en tienda');
  });

  it('CA-4-15 con agencia hay que elegir una de las activas, y recién entonces se pide el resumen con su costo y plazo', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[1].click();
    http().expectOne('/api/v1/agencias').flush(ok([{ id: 1, nombre: 'Shalom', activa: true, orden: 1 }, { id: 2, nombre: 'Olva', activa: true, orden: 2 }]));
    await p.estabilizar();
    http().expectNone('/api/v1/checkout/resumen');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);

    const select = p.el.querySelector<HTMLSelectElement>('.fs-agencia select')!;
    select.value = '1';
    select.dispatchEvent(new Event('change'));
    const resumen = http().expectOne('/api/v1/checkout/resumen');
    expect(resumen.request.body).toEqual({ modalidad: 'AGENCIA', direccionId: 1, agenciaId: 1 });
    resumen.flush(ok(resumenDe(137.8, { envio: { modalidad: 'AGENCIA', costo: 18, plazoDiasMin: 2, plazoDiasMax: 3, agenciaNombre: 'Shalom' } })));
    await p.estabilizar();

    expect(p.el.querySelector('.fs-resumen')?.textContent).toContain('S/ 18.00');
    expect(p.el.querySelector('.fs-resumen')?.textContent).toContain('2 a 3 días');
    expect(p.boton('Confirmar pedido').disabled).toBe(false);
  });

  it('CA-4-16 cada cambio de dirección recalcula el resumen con la API (sin reservar nada)', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);

    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    const resumen = http().expectOne('/api/v1/checkout/resumen');
    expect(resumen.request.body).toEqual({ modalidad: 'DELIVERY_LOCAL', direccionId: 2, agenciaId: null });
    resumen.flush(ok(resumenDe(127.8)));
    await p.estabilizar();

    expect(p.el.querySelector('.fs-resumen__total')?.textContent).toContain('S/ 127.80');
    http().expectNone('/api/v1/pedidos');
  });

  it('CA-4-16 la revisión del pedido muestra la foto de cada producto, y un ícono si no tiene', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);
    expect(p.el.querySelector('.ck-items li img')).toBeNull();

    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    const base = resumenDe(127.8);
    http()
      .expectOne('/api/v1/checkout/resumen')
      .flush(ok({ ...base, items: [{ ...base.items[0], imagenUrl: 'https://media.test/legging-400.jpg' }] }));
    await p.estabilizar();

    const foto = p.el.querySelector<HTMLImageElement>('.ck-items li img');
    expect(foto?.getAttribute('src')).toBe('https://media.test/legging-400.jpg');
    expect(foto?.getAttribute('alt')).toBe('Legging Pro');
  });

  it('CA-4-17 "Agregar dirección" abre el diálogo sobre el checkout; la nueva queda elegida y recalcula, y si cancelo nada cambia', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    abrirDialogo.mockResolvedValueOnce(undefined);
    p.boton('Agregar dirección').click();
    await p.estabilizar();
    expect(p.radios('direccion')).toHaveLength(1);
    http().expectNone('/api/v1/checkout/resumen');

    abrirDialogo.mockResolvedValueOnce(dir(5, 'Oficina', '070101'));
    p.boton('Agregar dirección').click();
    await vi.waitFor(() => http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES)));
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(129.8)));
    await p.estabilizar();

    expect(p.radios('direccion')).toHaveLength(2);
    expect(p.radios('direccion')[1].checked).toBe(true);
  });

  it('CA-4-17 con 10 direcciones el botón se deshabilita con el aviso del límite', async () => {
    const lista = Array.from({ length: 10 }, (_, i) => dir(i + 1, `Dir ${i + 1}`, '150122', i === 0));
    const p = await iniciar(lista);

    expect(p.boton('Agregar dirección').disabled).toBe(true);
    expect(p.el.textContent).toContain('Llegaste al máximo de 10 direcciones');
  });

  it('CA-4-17 sin direcciones solo se puede elegir recojo hasta agregar una', async () => {
    const p = await iniciar([]);

    expect(p.el.textContent).toContain('Aún no tienes direcciones');
    expect(p.radios('modalidad')).toHaveLength(1);
    expect(p.radios('modalidad')[0].checked).toBe(true);
    expect(p.el.querySelector('[aria-labelledby="sec-entrega"]')?.textContent).toContain('Recojo en tienda');
  });

  it('CA-4-18 si el envío no se puede calcular avisa y no deja confirmar', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);

    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    http().expectOne('/api/v1/checkout/resumen').flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), { status: 503, statusText: 'Service Unavailable' });
    await p.estabilizar();

    expect(p.el.textContent).toContain('No pudimos calcular el envío, inténtalo en un momento');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);
  });

  it('CA-4-62 si el carrito está vacío no dice "No pudimos calcular el envío": avisa que el carrito está vacío', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);

    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    http().expectOne('/api/v1/checkout/resumen').flush(errorApi('CARRITO_VACIO', 'Tu carrito está vacío'), { status: 400, statusText: 'Bad Request' });
    await p.estabilizar();

    expect(p.el.textContent).not.toContain('No pudimos calcular el envío');
    expect(p.el.textContent).toContain('Tu carrito está vacío');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);
  });

  function confirmable() {
    return iniciar([dir(1, 'Casa', '150122', true)]);
  }

  it('CA-4-19 confirmar manda la clave de idempotencia y el total visto, y lleva a pagar', async () => {
    const p = await confirmable();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const notas = p.el.querySelector<HTMLTextAreaElement>('textarea')!;
    notas.value = 'Tocar el timbre';
    notas.dispatchEvent(new Event('input'));

    p.boton('Confirmar pedido').click();
    const peticion = http().expectOne('/api/v1/pedidos');
    expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    expect(peticion.request.body).toEqual({
      modalidad: 'DELIVERY_LOCAL',
      direccionId: 1,
      agenciaId: null,
      notas: 'Tocar el timbre',
      totalEsperado: 129.8,
    });
    peticion.flush(ok({ codigo: 'FS-000124', total: 129.8, expiraEn: '2026-10-22T15:30:00Z' }), { status: 201, statusText: 'Created' });
    await p.estabilizar();

    expect(navegar).toHaveBeenCalledWith(['/pago', 'FS-000124']);
  });

  it('CA-4-20 mientras confirma el botón se deshabilita, y si se corta la red el reintento usa la misma clave', async () => {
    const p = await confirmable();

    p.boton('Confirmar pedido').click();
    await p.estabilizar();
    expect(p.boton('Confirmando').disabled).toBe(true);
    const primera = http().expectOne('/api/v1/pedidos');
    const clave = primera.request.headers.get('Idempotency-Key');
    primera.error(new ProgressEvent('error'));
    await p.estabilizar();

    p.boton('Confirmar pedido').click();
    const segunda = http().expectOne('/api/v1/pedidos');
    expect(segunda.request.headers.get('Idempotency-Key')).toBe(clave);
    segunda.flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), { status: 503, statusText: 'Service Unavailable' });
  });

  it('CA-4-21 si un precio cambió avisa "Los precios cambiaron" y actualiza el resumen', async () => {
    const p = await confirmable();

    p.boton('Confirmar pedido').click();
    http().expectOne('/api/v1/pedidos').flush(errorApi('PRECIO_CAMBIADO', 'Los precios cambiaron, revisa tu pedido'), { status: 409, statusText: 'Conflict' });
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(139.8)));
    await p.estabilizar();

    expect(p.el.textContent).toContain('Los precios cambiaron, revisa tu pedido');
    expect(p.el.querySelector('.fs-resumen__total')?.textContent).toContain('S/ 139.80');
  });

  it('CA-4-22 si una variante se agotó muestra "Sin stock para…" y recalcula', async () => {
    const p = await confirmable();

    p.boton('Confirmar pedido').click();
    http().expectOne('/api/v1/pedidos').flush(errorApi('SIN_STOCK', 'Sin stock para: Legging Pro M Negro'), { status: 409, statusText: 'Conflict' });
    http().expectOne('/api/v1/checkout/resumen').flush(errorApi('SIN_STOCK', 'Sin stock'), { status: 409, statusText: 'Conflict' });
    await p.estabilizar();

    expect(p.el.textContent).toContain('Sin stock para: Legging Pro M Negro');
  });

  it('CA-4-24 si un servicio no responde al confirmar dice "Inténtalo en un momento" y no navega', async () => {
    const p = await confirmable();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    p.boton('Confirmar pedido').click();
    http().expectOne('/api/v1/pedidos').flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), { status: 503, statusText: 'Service Unavailable' });
    await p.estabilizar();

    expect(p.el.textContent).toContain('Inténtalo en un momento');
    expect(navegar).not.toHaveBeenCalled();
  });

  it('CA-4-67 con agencia sin elegir avisa debajo del selector que debe seleccionar una y no deja confirmar', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[1].click();
    http().expectOne('/api/v1/agencias').flush(ok([{ id: 1, nombre: 'Shalom', activa: true, orden: 1 }]));
    await p.estabilizar();

    const campo = p.el.querySelector('.fs-agencia')!;
    const aviso = campo.querySelector('[role="alert"]');
    expect(aviso?.textContent).toContain('Selecciona una agencia');
    expect(campo.querySelector('select')?.getAttribute('aria-invalid')).toBe('true');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);
  });

  it('CA-4-67 con la agencia elegida desaparece el aviso y el resumen y "Revisa tu pedido" muestran su nombre', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[1].click();
    http().expectOne('/api/v1/agencias').flush(ok([{ id: 1, nombre: 'Shalom', activa: true, orden: 1 }]));
    await p.estabilizar();
    const select = p.el.querySelector<HTMLSelectElement>('.fs-agencia select')!;
    select.value = '1';
    select.dispatchEvent(new Event('change'));
    http()
      .expectOne('/api/v1/checkout/resumen')
      .flush(ok(resumenDe(137.8, { envio: { modalidad: 'AGENCIA', costo: 18, plazoDiasMin: 2, plazoDiasMax: 3, agenciaNombre: 'Shalom' } })));
    await p.estabilizar();

    expect(p.el.querySelector('.fs-agencia [role="alert"]')).toBeNull();
    expect(p.el.querySelector('.fs-resumen')?.textContent).toContain('Agencia Shalom');
    expect(p.el.querySelector('[aria-labelledby="sec-revision"]')?.textContent).toContain('Envío por agencia: Shalom');
  });

  it('CA-4-68 cada línea de "Revisa tu pedido" muestra precio base unitario, cantidad y el descuento de esa línea', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);
    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    const base = resumenDe(127.8);
    http()
      .expectOne('/api/v1/checkout/resumen')
      .flush(
        ok({
          ...base,
          descuentoTotal: 12,
          items: [{ ...base.items[0], descuentoPorcentaje: 10, descuentoMonto: 12, total: 107.8 }],
        }),
      );
    await p.estabilizar();

    const linea = p.el.querySelector('.ck-items li')?.textContent ?? '';
    expect(linea).toContain('Legging Pro');
    expect(linea).toContain('S/ 59.90 c/u');
    expect(linea).toContain('Cantidad: 2');
    expect(linea).toContain('Descuento (10 %): -S/ 12.00');
    expect(linea).toContain('S/ 107.80');
    expect(p.el.querySelector('.fs-resumen__fila--descuento')?.textContent).toContain('-S/ 12.00');
  });

  it('CA-4-68 una línea sin descuento no muestra el detalle de descuento', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);

    const linea = p.el.querySelector('.ck-items li')?.textContent ?? '';
    expect(linea).toContain('S/ 59.90 c/u');
    expect(linea).not.toContain('Descuento');
  });

  it('CA-4-70 con la agencia sin elegir "Revisa tu pedido" y el resumen siguen mostrando las líneas, el subtotal y el descuento del carrito', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[1].click();
    http().expectOne('/api/v1/agencias').flush(ok([{ id: 1, nombre: 'Shalom', activa: true, orden: 1 }]));
    await p.estabilizar();

    const revision = p.el.querySelector('[aria-labelledby="sec-revision"]')?.textContent ?? '';
    expect(revision).toContain('Legging Pro');
    expect(revision).toContain('S/ 59.90 c/u');
    expect(revision).toContain('Cantidad: 2');
    expect(revision).toContain('Descuento (10 %): -S/ 12.00');
    const resumen = p.el.querySelector('.fs-resumen')?.textContent ?? '';
    expect(resumen).toContain('S/ 119.80');
    expect(resumen).toContain('-S/ 12.00');
    expect(resumen).toContain('Elige una agencia');
    expect(resumen).toContain('Total sin envío');
    expect(resumen).toContain('S/ 107.80');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);
  });

  it('CA-4-70 si el resumen falla de verdad avisa que no pudo calcular el envío pero las líneas siguen visibles y no deja confirmar', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true), dir(2, 'Trabajo', '070101')]);

    p.radios('direccion')[1].click();
    http().expectOne('/api/v1/envios/opciones?ubigeo=070101').flush(ok(OPCIONES));
    http().expectOne('/api/v1/checkout/resumen').flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), { status: 503, statusText: 'Service Unavailable' });
    await p.estabilizar();

    expect(p.el.textContent).toContain('No pudimos calcular el envío, inténtalo en un momento');
    expect(p.el.querySelector('[aria-labelledby="sec-revision"]')?.textContent).toContain('Legging Pro');
    expect(p.el.querySelector('.fs-resumen')?.textContent).toContain('S/ 119.80');
    expect(p.boton('Confirmar pedido').disabled).toBe(true);
  });

  it('CA-4-70 con recojo en tienda las líneas y los montos se ven completos y se puede confirmar', async () => {
    const p = await iniciar([dir(1, 'Casa', '150122', true)]);
    p.radios('modalidad')[2].click();
    http().expectOne('/api/v1/checkout/resumen').flush(ok(resumenDe(107.8, { costoEnvio: 0, descuentoTotal: 12, envio: { modalidad: 'RECOJO', costo: 0 } })));
    await p.estabilizar();

    expect(p.el.querySelector('[aria-labelledby="sec-revision"]')?.textContent).toContain('Legging Pro');
    expect(p.el.querySelector('.fs-resumen')?.textContent).toContain('S/ 107.80');
    expect(p.el.querySelector('.fs-resumen')?.textContent).not.toContain('Total sin envío');
    expect(p.boton('Confirmar pedido').disabled).toBe(false);
  });

  it('CA-4-69 al confirmar el pedido el contador del carrito en la cabecera queda en 0', async () => {
    const p = await confirmable();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const contador = TestBed.inject(CarritoContador);
    contador.establecer(3);

    p.boton('Confirmar pedido').click();
    http().expectOne('/api/v1/pedidos').flush(ok({ codigo: 'FS-000124', total: 129.8, expiraEn: '2026-10-22T15:30:00Z' }), { status: 201, statusText: 'Created' });
    await p.estabilizar();

    expect(contador.cantidad()).toBe(0);
  });

  it('CA-4-69 si la confirmación falla el contador del carrito no cambia', async () => {
    const p = await confirmable();
    const contador = TestBed.inject(CarritoContador);
    contador.establecer(3);

    p.boton('Confirmar pedido').click();
    http().expectOne('/api/v1/pedidos').flush(errorApi('SERVICIO_NO_DISPONIBLE', 'No disponible'), { status: 503, statusText: 'Service Unavailable' });
    await p.estabilizar();

    expect(contador.cantidad()).toBe(3);
  });

  it('CA-4-24 si no se pueden cargar las direcciones ofrece reintentar', async () => {
    const m = await montar(CheckoutPage, {}, [...PROVEEDORES, { provide: DialogoDireccionService, useValue: { abrir: abrirDialogo } }]);
    http().expectOne('/api/v1/settings/public').flush(ok(configuracion()));
    http().expectOne('/api/v1/carrito').flush(ok(CARRITO_BASE));
    http().expectOne('/api/v1/clientes/me/direcciones').flush(errorApi('ERROR_INTERNO', 'Falló'), { status: 500, statusText: 'Error' });
    await m.estabilizar();

    expect(m.el.textContent).toContain('No pudimos preparar tu compra');
  });
});
