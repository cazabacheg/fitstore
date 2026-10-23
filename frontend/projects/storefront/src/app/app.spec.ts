import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CarritoContador, provideFitstoreHttp, provideSesionConfig } from 'shared';
import { App } from './app';

describe('App de la tienda', () => {
  it('CA-0b-02 se crea y tiene un router-outlet', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideFitstoreHttp({ carrito: true }),
        provideHttpClientTesting(),
        provideSesionConfig({ tipo: 'CLIENTE' }),
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    TestBed.inject(HttpTestingController).match('/api/v1/carrito');
    expect((fixture.nativeElement as HTMLElement).querySelector('router-outlet')).not.toBeNull();
  });

  it('CA-4-61 al arrancar la app carga el carrito para que el contador de la bolsa no quede en 0', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideFitstoreHttp({ carrito: true }),
        provideHttpClientTesting(),
        provideSesionConfig({ tipo: 'CLIENTE' }),
      ],
    }).compileComponents();
    TestBed.createComponent(App);

    const peticion = TestBed.inject(HttpTestingController).expectOne('/api/v1/carrito');
    peticion.flush({ success: true, mensaje: 'ok', data: { lineas: [], avisosTramo: [], subtotal: 0, descuentoTotal: 0, total: 0, cantidadTotal: 3, preciosPorConfirmar: false, puedeIrAlCheckout: true } });

    expect(TestBed.inject(CarritoContador).cantidad()).toBe(3);
  });
});
