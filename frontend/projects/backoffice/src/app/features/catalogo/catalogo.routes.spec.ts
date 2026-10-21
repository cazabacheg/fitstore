import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { montar } from '../../testing/montar';

describe('Rutas del catálogo', () => {
  it('CA-2-28 el OPERADOR no entra a /catalogo/productos/nuevo (solo ADMIN)', async () => {
    await montar(SESION_OPERADOR, { url: '/catalogo/productos/nuevo' });
    expect(TestBed.inject(Router).url).toBe('/403');
  });

  it('CA-2-21 el ADMIN entra a /catalogo/productos/nuevo', async () => {
    await montar(SESION_ADMIN, { url: '/catalogo/productos/nuevo' });
    expect(TestBed.inject(Router).url).toBe('/catalogo/productos/nuevo');
  });

  it('CA-2-28 el OPERADOR sí abre un producto existente (solo lectura)', async () => {
    await montar(SESION_OPERADOR, { url: '/catalogo/productos/7' });
    expect(TestBed.inject(Router).url).toBe('/catalogo/productos/7');
  });
});
