import { TestBed } from '@angular/core/testing';
import { CONTADOR_MENU, provideContadorMenu, type ContadorMenu } from '../menu/contador-menu';
import { CarritoContador } from './carrito-contador';

describe('CarritoContador', () => {
  it('CA-0b-23 empieza en 0 y la spec del carrito lo actualiza sin recargar', () => {
    const contador = TestBed.inject(CarritoContador);
    expect(contador.cantidad()).toBe(0);

    contador.establecer(3);
    expect(contador.cantidad()).toBe(3);

    contador.establecer(0);
    expect(contador.cantidad()).toBe(0);
  });

  it('CA-0b-23 ignora cantidades negativas o con decimales', () => {
    const contador = TestBed.inject(CarritoContador);
    contador.establecer(-4);
    expect(contador.cantidad()).toBe(0);
    contador.establecer(2.7);
    expect(contador.cantidad()).toBe(2);
  });
});

describe('CONTADOR_MENU', () => {
  it('CA-0b-24 cada funcionalidad registra su contador por clave sin tocar el shell', () => {
    TestBed.configureTestingModule({
      providers: [
        provideContadorMenu('reembolsos', () => 4),
        provideContadorMenu('pedidos', () => 9),
      ],
    });

    const contadores = TestBed.inject(CONTADOR_MENU) as ContadorMenu[];

    expect(contadores.map((c) => [c.clave, c.contar()])).toEqual([
      ['reembolsos', 4],
      ['pedidos', 9],
    ]);
  });
});
