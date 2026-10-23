import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CarritoContador } from 'shared';
import { vi } from 'vitest';
import { montar } from '../../testing';
import { CarritoPage, ESPERA_CANTIDAD_MS } from './carrito.page';
import { CarritoService } from './carrito.service';
import { PROVEEDORES, carrito, conTramo, errorApi, http, linea, ok } from './pruebas-carrito';

const RUTA = '/api/v1/carrito';

async function iniciar(datos: ReturnType<typeof carrito>) {
  const m = await montar(CarritoPage, {}, PROVEEDORES);
  http().expectOne(RUTA).flush(ok(datos));
  await m.estabilizar();
  const boton = (texto: string) =>
    [...m.el.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  const porEtiqueta = (etiqueta: string) =>
    m.el.querySelector<HTMLButtonElement>(`button[aria-label="${etiqueta}"]`)!;
  return { ...m, boton, porEtiqueta };
}

describe('Carrito (P-4-01)', () => {
  afterEach(() => {
    vi.useRealTimers();
    http().verify();
  });

  it('CA-4-01 agregar una variante actualiza el contador de la cabecera con el total de unidades', async () => {
    TestBed.configureTestingModule({ providers: PROVEEDORES });
    const servicio = TestBed.inject(CarritoService);
    const contador = TestBed.inject(CarritoContador);

    servicio.agregar(101, 2).subscribe();
    const peticion = http().expectOne(`${RUTA}/items`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ varianteId: 101, cantidad: 2 });
    peticion.flush(ok(carrito([linea(101, 'Legging Pro', 2)])));

    expect(contador.cantidad()).toBe(2);
    expect(servicio.carrito()?.lineas).toHaveLength(1);
    http().verify();
  });

  it('CA-4-02 muestra cada línea con foto, talla, color, precio, descuento y total, y los totales sin envío', async () => {
    const p = await iniciar(
      carrito(
        [
          linea(101, 'Legging Pro', 4, {
            imagenUrl: 'http://media/1.jpg',
            descuentoPorcentaje: 10,
            descuentoMonto: 23.96,
            total: 215.64,
          }),
          linea(230, 'Top Flex', 1, { precioUnitario: 39.9, total: 39.9 }),
        ],
        {
          avisosTramo: [
            {
              productoId: 10,
              producto: 'Legging Pro',
              unidades: 4,
              tramoAlcanzado: { unidadesMinimas: 3, porcentaje: 10 },
              siguienteTramo: { unidadesMinimas: 6, porcentaje: 15, faltan: 2 },
            },
          ],
        },
      ),
    );

    const texto = p.el.textContent ?? '';
    expect(p.el.querySelectorAll('.fs-linea')).toHaveLength(2);
    expect(p.el.querySelector('img')?.getAttribute('src')).toBe('http://media/1.jpg');
    expect(texto).toContain('Legging Pro');
    expect(texto).toContain('M · Negro');
    expect(texto).toContain('S/ 59.90 c/u');
    expect(texto).toContain('Ahorras S/ 23.96 (10 %)');
    expect(texto).toContain('S/ 215.64');
    expect(texto).toContain('Subtotal');
    expect(texto).toContain('Descuento');
    expect(texto).toContain('El envío se calcula en el checkout');
    expect(texto).toContain('agrega 2 más y ahorra 15 %');
  });

  it('CA-4-03 al cambiar la cantidad espera 400 ms y recalcula con la API; quitar y vaciar (con confirmación) la limpian', async () => {
    const p = await iniciar(carrito([linea(101, 'Legging Pro', 2), linea(230, 'Top Flex', 1)]));
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });

    p.porEtiqueta('Agregar una unidad').click();
    p.porEtiqueta('Agregar una unidad').click();
    http().expectNone(`${RUTA}/items/101`);
    vi.advanceTimersByTime(ESPERA_CANTIDAD_MS);
    const cambio = http().expectOne(`${RUTA}/items/101`);
    expect(cambio.request.method).toBe('PUT');
    expect(cambio.request.body).toEqual({ cantidad: 4 });
    cambio.flush(ok(carrito([linea(101, 'Legging Pro', 4), linea(230, 'Top Flex', 1)])));
    await p.estabilizar();
    expect(p.el.querySelector('.fs-linea__unidades')?.textContent).toBe('4');

    const quitar = p.el.querySelector<HTMLButtonElement>('button[aria-label="Quitar Top Flex"]')!;
    quitar.click();
    const borrado = http().expectOne(`${RUTA}/items/230`);
    expect(borrado.request.method).toBe('DELETE');
    borrado.flush(ok(carrito([linea(101, 'Legging Pro', 4)])));
    await p.estabilizar();
    expect(p.el.querySelectorAll('.fs-linea')).toHaveLength(1);

    p.boton('Vaciar carrito').click();
    await p.estabilizar();
    http().expectNone(RUTA);
    [...p.el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')]
      .find((b) => b.textContent?.includes('Vaciar carrito'))!
      .click();
    const vaciar = http().expectOne(RUTA);
    expect(vaciar.request.method).toBe('DELETE');
    vaciar.flush(ok(carrito([])));
    await p.estabilizar();
    expect(p.el.textContent).toContain('Tu carrito está vacío');
  });

  it('CA-4-04 si no alcanza el stock muestra "Solo quedan 3 unidades" y la línea conserva su cantidad', async () => {
    const p = await iniciar(carrito([linea(101, 'Legging Pro', 2)]));
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });

    p.porEtiqueta('Agregar una unidad').click();
    await p.estabilizar();
    expect(p.el.querySelector('.fs-linea__unidades')?.textContent).toBe('3');
    vi.advanceTimersByTime(ESPERA_CANTIDAD_MS);
    http()
      .expectOne(`${RUTA}/items/101`)
      .flush(errorApi('STOCK_INSUFICIENTE', 'Solo quedan 3 unidades', { disponible: 3 }), {
        status: 409,
        statusText: 'Conflict',
      });
    await p.estabilizar();

    expect(p.el.querySelector('.fs-linea__error')?.textContent).toContain('Solo quedan 3 unidades');
    expect(p.el.querySelector('.fs-linea__unidades')?.textContent).toBe('2');
  });

  it('CA-4-05 no deja pasar de 100 unidades y muestra el límite que responde la API', async () => {
    const p = await iniciar(carrito([linea(101, 'Legging Pro', 100), linea(230, 'Top Flex', 99)]));

    expect(p.porEtiqueta('Agregar una unidad').disabled).toBe(true);

    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    p.el.querySelectorAll<HTMLButtonElement>('button[aria-label="Agregar una unidad"]')[1].click();
    vi.advanceTimersByTime(ESPERA_CANTIDAD_MS);
    http()
      .expectOne(`${RUTA}/items/230`)
      .flush(errorApi('LIMITE_CARRITO', 'Máximo 100 unidades por producto'), {
        status: 400,
        statusText: 'Bad Request',
      });
    await p.estabilizar();
    expect(p.el.querySelector('.fs-linea__error')?.textContent).toContain('Máximo 100 unidades');
  });

  it('CA-4-06 una línea que ya no está disponible se atenúa con "Quitar" y bloquea el checkout', async () => {
    const p = await iniciar(
      carrito([linea(101, 'Legging Pro', 1), linea(230, 'Top Flex', 1, { estado: 'NO_DISPONIBLE' })], {
        puedeIrAlCheckout: false,
      }),
    );
    const atenuada = p.el.querySelector<HTMLElement>('.fs-linea--no-disponible')!;
    expect(atenuada.textContent).toContain('Ya no está disponible');
    expect(atenuada.style.opacity).toBe('0.5');
    expect(p.boton('Ir al checkout').disabled).toBe(true);
    expect(p.el.querySelector('a[href="/checkout"]')).toBeNull();

    [...atenuada.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Quitar'))!.click();
    expect(http().expectOne(`${RUTA}/items/230`).request.method).toBe('DELETE');
  });

  it('CA-4-07 si catalog no responde muestra "Precios por confirmar" y el checkout queda deshabilitado', async () => {
    const p = await iniciar(
      carrito([linea(101, 'Legging Pro', 1)], { preciosPorConfirmar: true, puedeIrAlCheckout: false }),
    );

    expect(p.el.textContent).toContain('Precios por confirmar');
    expect(p.boton('Ir al checkout').disabled).toBe(true);
  });

  it('CA-4-08 un carrito vacío dice "Tu carrito está vacío" con un enlace a la tienda', async () => {
    const p = await iniciar(carrito([]));

    expect(p.el.textContent).toContain('Tu carrito está vacío');
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    p.boton('Ir a la tienda').click();
    expect(navegar).toHaveBeenCalledWith('/productos');
  });

  it('CA-4-02 si la API falla ofrece reintentar', async () => {
    const m = await montar(CarritoPage, {}, PROVEEDORES);
    http().expectOne(RUTA).flush(errorApi('ERROR_INTERNO', 'Falló'), { status: 500, statusText: 'Error' });
    await m.estabilizar();

    expect(m.el.textContent).toContain('No pudimos cargar tu carrito');
  });
  it('CA-4-63 al llegar al stock que informó el servidor el "+" se deshabilita y no se sugiere un tramo inalcanzable', async () => {
    const p = await iniciar(conTramo(2));
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    expect(p.el.textContent).toContain('agrega 1 más y ahorra 10 %');

    p.porEtiqueta('Agregar una unidad').click();
    vi.advanceTimersByTime(ESPERA_CANTIDAD_MS);
    http()
      .expectOne(`${RUTA}/items/101`)
      .flush(errorApi('STOCK_INSUFICIENTE', 'Solo quedan 2 unidades', { disponible: 2 }), {
        status: 409,
        statusText: 'Conflict',
      });
    await p.estabilizar();

    expect(p.el.querySelector('.fs-linea__unidades')?.textContent).toBe('2');
    expect(p.porEtiqueta('Agregar una unidad').disabled).toBe(true);
    expect(p.el.textContent).not.toContain('ahorra 10 %');
  });

  it('CA-4-63 una línea con STOCK_INSUFICIENTE y disponible deshabilita el "+" desde que se carga', async () => {
    const p = await iniciar(conTramo(3, { estado: 'STOCK_INSUFICIENTE', disponible: 3 }));

    expect(p.porEtiqueta('Agregar una unidad').disabled).toBe(true);
  });

  it('CA-4-64 si el descuento total es 0 la fila "Descuento" no aparece, y con descuento sí', async () => {
    const sin = await iniciar(carrito([linea(101, 'Legging Pro', 2)]));
    expect(sin.el.textContent).not.toContain('Descuento');
    expect(sin.el.textContent).not.toContain('-S/ 0.00');
    TestBed.resetTestingModule();

    const con = await iniciar(conTramo(3));
    expect(con.el.textContent).toContain('Descuento');
    expect(con.el.textContent).toContain('-S/ 17.97');
  });

  it('CA-4-65 "Quitar" es un ícono de tacho con nombre accesible, no un texto', async () => {
    const p = await iniciar(carrito([linea(101, 'Legging Pro', 1)]));

    const quitar = p.el.querySelector<HTMLButtonElement>('button[aria-label="Quitar Legging Pro"]')!;
    expect(quitar.title).toBe('Quitar');
    expect(quitar.querySelector('.fs-icono')?.textContent?.trim()).toBe('delete');
    expect(quitar.textContent?.replace('delete', '').trim()).toBe('');
    quitar.click();
    expect(http().expectOne(`${RUTA}/items/101`).request.method).toBe('DELETE');
  });

  it('CA-4-66 al tocar "+" el descuento y los totales se ven al instante, sin esperar a la API', async () => {
    const p = await iniciar(conTramo(2));
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });

    p.porEtiqueta('Agregar una unidad').click();
    await p.estabilizar();

    expect(p.el.querySelector('.fs-linea__unidades')?.textContent).toBe('3');
    expect(p.el.textContent).toContain('Ahorras S/ 17.97 (10 %)');
    expect(p.el.textContent).toContain('S/ 161.73');
    http().expectNone(`${RUTA}/items/101`);

    vi.advanceTimersByTime(ESPERA_CANTIDAD_MS);
    const cambio = http().expectOne(`${RUTA}/items/101`);
    expect(cambio.request.body).toEqual({ cantidad: 3 });
    cambio.flush(ok(conTramo(3, { descuentoPorcentaje: 12, descuentoMonto: 21.56, total: 158.14 })));
    await p.estabilizar();
    expect(p.el.textContent).toContain('Ahorras S/ 21.56 (12 %)');
  });
});
