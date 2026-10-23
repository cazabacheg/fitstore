import { TestBed } from '@angular/core/testing';
import { CarritoContador, SesionStore } from 'shared';
import { SESION_CLIENTE } from '../../testing';
import { CarritoService } from './carrito.service';
import { PROVEEDORES, carrito, conTramo, errorApi, http, linea, ok } from './pruebas-carrito';

const RUTA = '/api/v1/carrito';
const ID = '3f6b2a10-7c4e-4a52-9d1b-0e8f5a6c7d21';

function crear() {
  localStorage.clear();
  TestBed.configureTestingModule({ providers: PROVEEDORES });
  return {
    servicio: TestBed.inject(CarritoService),
    contador: TestBed.inject(CarritoContador),
    sesion: TestBed.inject(SesionStore),
  };
}

describe('CarritoService', () => {
  afterEach(() => http().verify());

  it('CA-4-61 al arrancar sin sesión carga el carrito anónimo y el contador de la bolsa lo refleja', () => {
    const { servicio, contador } = crear();

    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(carrito([linea(101, 'Legging Pro', 2), linea(230, 'Top Flex', 1)])));

    expect(contador.cantidad()).toBe(3);
  });

  it('CA-4-61 al arrancar con sesión recuperada carga el carrito del cliente', () => {
    const { servicio, contador, sesion } = crear();
    sesion.iniciarSesion(SESION_CLIENTE);

    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(carrito([linea(101, 'Legging Pro', 4)])));

    expect(contador.cantidad()).toBe(4);
  });

  it('CA-4-61 al cerrar sesión el contador vuelve a 0 y se carga el carrito anónimo', () => {
    const { servicio, contador, sesion } = crear();
    sesion.iniciarSesion(SESION_CLIENTE);
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(carrito([linea(101, 'Legging Pro', 4)])));
    expect(contador.cantidad()).toBe(4);

    sesion.cerrarSesion();
    TestBed.tick();

    expect(contador.cantidad()).toBe(0);
    http().expectOne(RUTA).flush(ok(carrito([])));
    expect(servicio.vacio()).toBe(true);
  });

  it('CA-4-10 al iniciar sesión con carrito anónimo llama UNA vez a /fusionar, muestra el resultado y borra el carritoId', () => {
    const { servicio, contador } = crear();
    localStorage.setItem('fs_carrito_id', ID);
    let terminado = false;

    servicio.alIniciarSesion().subscribe(() => (terminado = true));
    const fusion = http().expectOne(`${RUTA}/fusionar`);
    expect(fusion.request.method).toBe('POST');
    expect(fusion.request.headers.get('X-Carrito-Id')).toBe(ID);
    expect(terminado).toBe(false);
    fusion.flush(
      ok(carrito([linea(101, 'Legging Pro', 3)], { avisos: ['La cantidad de Legging Pro se ajustó a 100'] })),
    );

    expect(terminado).toBe(true);
    expect(contador.cantidad()).toBe(3);
    expect(servicio.avisos()).toEqual(['La cantidad de Legging Pro se ajustó a 100']);
    expect(localStorage.getItem('fs_carrito_id')).toBeNull();
  });

  it('CA-4-10 sin carrito anónimo no fusiona: solo carga el carrito del cliente para el contador', () => {
    const { servicio, contador } = crear();

    servicio.alIniciarSesion().subscribe();
    http().expectNone(`${RUTA}/fusionar`);
    http().expectOne(RUTA).flush(ok(carrito([linea(101, 'Legging Pro', 2)])));

    expect(contador.cantidad()).toBe(2);
  });

  it('CA-4-12 si la fusión falla no bloquea el ingreso: termina, conserva el carritoId y recarga el carrito', () => {
    const { servicio } = crear();
    localStorage.setItem('fs_carrito_id', ID);
    let terminado = false;

    servicio.alIniciarSesion().subscribe(() => (terminado = true));
    http()
      .expectOne(`${RUTA}/fusionar`)
      .flush(errorApi('ERROR_INTERNO', 'Falló'), { status: 500, statusText: 'Error' });
    http().expectOne(RUTA).flush(ok(carrito([])));

    expect(terminado).toBe(true);
    expect(localStorage.getItem('fs_carrito_id')).toBe(ID);
  });

  it('CA-4-66 previsualizar recalcula cantidad, subtotal, descuento y total con los tramos del servidor, y el contador', () => {
    const { servicio, contador } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(2)));

    servicio.previsualizar(101, 3);

    const l = servicio.carrito()!.lineas[0];
    expect(l.cantidad).toBe(3);
    expect(l.descuentoPorcentaje).toBe(10);
    expect(l.descuentoMonto).toBe(17.97);
    expect(l.total).toBe(161.73);
    expect(servicio.carrito()!.subtotal).toBe(179.7);
    expect(servicio.carrito()!.descuentoTotal).toBe(17.97);
    expect(servicio.carrito()!.total).toBe(161.73);
    expect(contador.cantidad()).toBe(3);
    http().expectNone(`${RUTA}/items/101`);
  });

  it('CA-4-66 al bajar de un tramo que el navegador ya conoce, el descuento desaparece al instante', () => {
    const { servicio } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(3)));

    servicio.previsualizar(101, 2);

    expect(servicio.carrito()!.lineas[0].descuentoPorcentaje).toBe(0);
    expect(servicio.carrito()!.descuentoTotal).toBe(0);
    expect(servicio.carrito()!.total).toBe(119.8);
  });

  it('CA-4-66 el navegador manda solo la cantidad y la respuesta del servidor siempre manda', () => {
    const { servicio } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(2)));
    servicio.previsualizar(101, 3);

    servicio.cambiarCantidad(101, 3);
    const peticion = http().expectOne(`${RUTA}/items/101`);
    expect(peticion.request.body).toEqual({ cantidad: 3 });
    peticion.flush(ok(conTramo(3, { descuentoPorcentaje: 12, descuentoMonto: 21.56, total: 158.14 })));

    expect(servicio.carrito()!.lineas[0].descuentoPorcentaje).toBe(12);
    expect(servicio.carrito()!.lineas[0].total).toBe(158.14);
  });

  it('CA-4-66 si el servidor rechaza vuelve a lo último confirmado y muestra el error (CA-4-04)', () => {
    const { servicio, contador } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(2)));
    servicio.previsualizar(101, 5);

    servicio.cambiarCantidad(101, 5);
    http()
      .expectOne(`${RUTA}/items/101`)
      .flush(errorApi('STOCK_INSUFICIENTE', 'Solo quedan 3 unidades', { disponible: 3 }), {
        status: 409,
        statusText: 'Conflict',
      });

    expect(servicio.carrito()!.lineas[0].cantidad).toBe(2);
    expect(contador.cantidad()).toBe(2);
    expect(servicio.errores()[101]).toContain('Solo quedan 3 unidades');
    expect(servicio.stockMaximo(101)).toBe(3);
  });

  it('CA-4-63 el stock disponible sale del rechazo del servidor o de la línea con STOCK_INSUFICIENTE', () => {
    const { servicio } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(5, { estado: 'STOCK_INSUFICIENTE', disponible: 4 })));

    expect(servicio.stockMaximo(101)).toBe(4);
    expect(servicio.stockMaximo(999)).toBeNull();
  });

  it('CA-4-63 el aviso del siguiente tramo se oculta si el stock no alcanza para llegar', () => {
    const { servicio } = crear();
    servicio.iniciarApp();
    http().expectOne(RUTA).flush(ok(conTramo(2)));
    expect(servicio.tramoAlcanzable(servicio.carrito()!.avisosTramo[0])).toBe(true);

    servicio.cambiarCantidad(101, 3);
    http()
      .expectOne(`${RUTA}/items/101`)
      .flush(errorApi('STOCK_INSUFICIENTE', 'Solo quedan 2 unidades', { disponible: 2 }), {
        status: 409,
        statusText: 'Conflict',
      });

    expect(servicio.stockMaximo(101)).toBe(2);
    expect(servicio.tramoAlcanzable(servicio.carrito()!.avisosTramo[0])).toBe(false);
  });
});
