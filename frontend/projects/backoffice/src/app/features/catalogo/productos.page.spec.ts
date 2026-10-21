import { Component } from '@angular/core';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { ProductosPage } from './productos.page';
import { RESUMENES, armar, asentar, atender, boton, itemsMenu, pagina } from './prueba-catalogo';

@Component({ template: 'formulario' })
class Falso {}

const RUTAS = [
  { path: 'catalogo/productos', component: ProductosPage },
  { path: 'catalogo/productos/nuevo', component: Falso },
  { path: 'catalogo/productos/:id', component: Falso },
];
const LISTA = { '/admin/productos': pagina(RESUMENES) };

async function abrir(sesion = SESION_ADMIN, url = '/catalogo/productos') {
  const t = await armar(RUTAS, url, sesion);
  atender(t.ctrl, LISTA);
  await asentar(t.fixture);
  return t;
}

describe('ProductosPage (P-2-03)', () => {
  it('CA-2-21 el nombre del producto es un enlace al formulario y hay botón "Nuevo producto" para ADMIN', async () => {
    const { el } = await abrir();
    const enlace = el.querySelector<HTMLAnchorElement>('a.fs-codigo')!;
    expect(enlace.textContent).toContain('Legging Pro');
    expect(enlace.getAttribute('href')).toBe('/catalogo/productos/7');
    expect(boton(el, 'Nuevo producto')!.getAttribute('href')).toBe('/catalogo/productos/nuevo');
  });

  it('CA-2-25 el ADMIN ve el menú "⋮" con Editar, Publicar o Desactivar y Eliminar', async () => {
    const { el, fixture } = await abrir();
    el.querySelectorAll<HTMLButtonElement>('button[aria-label="Más acciones"]')[0].click();
    await asentar(fixture);
    expect(itemsMenu()).toEqual(['Editar', 'Publicar', 'Eliminar']);
  });

  it('CA-2-25 publicar desde el menú llama a PATCH publicar y recarga', async () => {
    const { el, fixture, ctrl } = await abrir();
    el.querySelectorAll<HTMLButtonElement>('button[aria-label="Más acciones"]')[0].click();
    await asentar(fixture);
    document.querySelectorAll<HTMLElement>('button[mat-menu-item]')[1].click();
    await asentar(fixture);
    const req = ctrl.expectOne('/api/v1/admin/productos/7/publicar');
    expect(req.request.method).toBe('PATCH');
  });

  it('CA-2-26 eliminar pide confirmación antes de llamar a DELETE', async () => {
    const { el, fixture, ctrl } = await abrir();
    el.querySelectorAll<HTMLButtonElement>('button[aria-label="Más acciones"]')[0].click();
    await asentar(fixture);
    document.querySelectorAll<HTMLElement>('button[mat-menu-item]')[2].click();
    await asentar(fixture);
    ctrl.expectNone('/api/v1/admin/productos/7');
    const confirmar = document.querySelector<HTMLElement>('mat-dialog-container button.fs-btn--peligro')!;
    expect(confirmar.textContent).toContain('Eliminar');
    confirmar.click();
    await asentar(fixture);
    expect(ctrl.expectOne('/api/v1/admin/productos/7').request.method).toBe('DELETE');
  });

  it('CA-2-28 el OPERADOR solo ve "Ver ›" y no puede crear', async () => {
    const { el } = await abrir(SESION_OPERADOR);
    expect(boton(el, 'Nuevo producto')).toBeUndefined();
    expect(el.querySelector('button[aria-label="Más acciones"]')).toBeNull();
    const ver = Array.from(el.querySelectorAll('a')).filter((a) => a.textContent?.includes('Ver ›'));
    expect(ver.length).toBe(2);
    expect(ver[0].getAttribute('href')).toBe('/catalogo/productos/7');
  });

  it('CA-2-21 el estado y el buscador de la URL viajan a la API', async () => {
    const t = await armar(RUTAS, '/catalogo/productos?q=leg&estado=ACTIVO');
    const req = t.ctrl.expectOne((r) => r.url === '/api/v1/admin/productos');
    expect(req.request.params.get('q')).toBe('leg');
    expect(req.request.params.get('estado')).toBe('ACTIVO');
    req.flush({ success: true, mensaje: 'ok', data: pagina(RESUMENES) });
  });
});
