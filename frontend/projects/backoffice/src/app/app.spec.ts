import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp } from 'shared';
import { App } from './app';

describe('App del backoffice', () => {
  it('CA-0b-02 se crea y tiene un router-outlet', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('router-outlet')).not.toBeNull();
  });
});

describe('Errores de la API en el backoffice (CA-0b-13)', () => {
  async function montarApp() {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideFitstoreHttp({ carrito: false }),
        provideHttpClientTesting(),
      ],
    });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture;
  }

  it('CA-0b-13 un error con formato ApiResponse se ve en un toast', async () => {
    const fixture = await montarApp();
    TestBed.inject(HttpClient)
      .get('/api/v1/pedidos')
      .subscribe({ error: () => undefined });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/pedidos')
      .flush(
        { success: false, mensaje: 'No hay stock suficiente', codigo: 'SIN_STOCK' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent,
    ).toContain('No hay stock suficiente');
  });

  it('CA-0b-13 un 503 muestra el banner "Servicio no disponible" sin romper el resto', async () => {
    const fixture = await montarApp();
    TestBed.inject(HttpClient)
      .get('/api/v1/pedidos')
      .subscribe({ error: () => undefined });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/pedidos')
      .flush(
        { success: false, mensaje: 'Servicio no disponible', codigo: 'SERVICIO_NO_DISPONIBLE' },
        { status: 503, statusText: 'Unavailable' },
      );
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('fs-banner-servicio-no-disponible')?.textContent).toContain(
      'Servicio no disponible',
    );
    expect(el.querySelector('router-outlet')).not.toBeNull();
  });
});
