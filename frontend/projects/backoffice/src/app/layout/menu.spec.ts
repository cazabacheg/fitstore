import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { MENU, hojasDelMenu, menuParaRol } from './menu';
import { montar } from '../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../testing/sesiones';

const ETIQUETAS_SOLO_ADMIN = [
  'Pagos',
  'Tarifas y agencias',
  'Staff',
  'Notificaciones',
  'Configuración de tienda',
];

describe('Menú del backoffice por rol (R3)', () => {
  it('CA-0b-17 el OPERADOR no ve Pagos, Tarifas y agencias, Staff, Notificaciones ni Configuración de tienda', () => {
    const etiquetas = menuParaRol('OPERADOR').map((i) => i.etiqueta);
    for (const prohibida of ETIQUETAS_SOLO_ADMIN) expect(etiquetas).not.toContain(prohibida);
    expect(etiquetas).toEqual([
      'Dashboard',
      'Pedidos',
      'Entregas',
      'Repartidores',
      'Catálogo',
      'Inventario',
      'Clientes',
      'Reclamos',
    ]);
  });

  it('CA-0b-17 el OPERADOR ve en Catálogo solo el subítem Productos', () => {
    const catalogo = menuParaRol('OPERADOR').find((i) => i.clave === 'catalogo');
    expect(catalogo?.subitems?.map((s) => s.etiqueta)).toEqual(['Productos']);
  });

  it('CA-0b-24 el ADMIN ve los 13 ítems de R3 con sus subítems', () => {
    const menu = menuParaRol('ADMIN');
    expect(menu.map((i) => i.etiqueta)).toEqual([
      'Dashboard',
      'Pedidos',
      'Entregas',
      'Repartidores',
      'Catálogo',
      'Inventario',
      'Pagos',
      'Tarifas y agencias',
      'Clientes',
      'Staff',
      'Notificaciones',
      'Reclamos',
      'Configuración de tienda',
    ]);
    const subs = (clave: string) =>
      menu.find((i) => i.clave === clave)?.subitems?.map((s) => s.etiqueta);
    expect(subs('entregas')).toEqual(['Lista', 'Del día']);
    expect(subs('catalogo')).toEqual([
      'Productos',
      'Categorías',
      'Tallas y colores',
      'Descuentos',
      'Carga masiva',
    ]);
    expect(subs('pagos')).toEqual(['Pagos', 'Reembolsos']);
    expect(subs('tarifas-agencias')).toEqual(['Tarifas', 'Agencias']);
    expect(subs('notificaciones')).toEqual(['Registro', 'Plantillas']);
    expect(subs('configuracion')).toEqual(['Tienda', 'Políticas']);
  });

  it('CA-0b-17 el menú y las rutas coinciden: cada hoja da 403 a quien el menú oculta', async () => {
    const harness = await montar(SESION_OPERADOR);
    const router = TestBed.inject(Router);
    const permitidas = new Set(hojasDelMenu(menuParaRol('OPERADOR')).map((h) => h.ruta));
    for (const hoja of hojasDelMenu(MENU)) {
      await harness.navigateByUrl('/403');
      await harness.navigateByUrl(hoja.ruta);
      expect(router.url, hoja.ruta).toBe(permitidas.has(hoja.ruta) ? hoja.ruta : '/403');
    }
  });

  it('CA-0b-24 todas las hojas del menú del ADMIN abren su ruta', async () => {
    const harness = await montar(SESION_ADMIN);
    const router = TestBed.inject(Router);
    for (const hoja of hojasDelMenu(MENU)) {
      await harness.navigateByUrl('/403');
      await harness.navigateByUrl(hoja.ruta);
      expect(router.url, hoja.ruta).toBe(hoja.ruta);
    }
  });
});
