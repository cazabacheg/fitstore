import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  Component,
  EnvironmentProviders,
  Provider,
  Type,
  provideZonelessChangeDetection,
} from '@angular/core';
import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter, type Routes } from '@angular/router';
import {
  SesionStore,
  provideFitstoreHttp,
  provideSesionConfig,
  type authApi,
  type catalogApi,
} from 'shared';
import { SESION_ADMIN } from '../../testing/sesiones';

/** Ayudas compartidas por las pruebas del catálogo y el inventario del backoffice (no forman parte de la aplicación). */
export const sobre = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

@Component({ selector: 'fs-anfitrion', imports: [RouterOutlet], template: '<router-outlet />' })
class Anfitrion {}

/** Deja correr efectos y peticiones (sin `whenStable`: una petición pendiente lo bloquearía). */
export async function asentar(fixture: ComponentFixture<unknown>): Promise<void> {
  fixture.detectChanges();
  await new Promise((r) => setTimeout(r));
  fixture.detectChanges();
}

export async function armar(rutas: Routes, url: string, sesion: authApi.Sesion = SESION_ADMIN) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter(rutas),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  TestBed.inject(SesionStore).iniciarSesion(sesion);
  const ctrl = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(Anfitrion);
  const router = TestBed.inject(Router);
  await router.navigateByUrl(url);
  await asentar(fixture);
  return { ctrl, fixture, router, el: fixture.nativeElement as HTMLElement };
}

/** Atiende todas las peticiones pendientes: la URL (sin consulta ni `/api/v1`) busca su respuesta en el mapa. */
export function atender(
  ctrl: HttpTestingController,
  respuestas: Record<string, unknown> = {},
  errores: Record<string, { status: number; cuerpo?: unknown }> = {},
) {
  const atendidas = ctrl.match(() => true);
  for (const req of atendidas) {
    const ruta = req.request.url.split('?')[0].replace('/api/v1', '');
    const error = errores[ruta];
    if (error)
      req.flush(error.cuerpo ?? { success: false, mensaje: 'Error', codigo: 'ERROR' }, {
        status: error.status,
        statusText: 'x',
      });
    else req.flush(sobre(respuestas[ruta] ?? null));
  }
  return atendidas;
}

export const pagina = <T>(contenido: T[]) => ({
  pagina: 0,
  tamano: 20,
  totalElementos: contenido.length,
  totalPaginas: 1,
  contenido,
});

export const boton = (raiz: ParentNode, texto: string) =>
  Array.from(raiz.querySelectorAll('button, a')).find((b) => b.textContent?.includes(texto)) as
    HTMLElement | undefined;

/** Ítems del menú "⋮" (el panel vive en la capa superpuesta). */
export const itemsMenu = () =>
  Array.from(document.querySelectorAll<HTMLElement>('button[mat-menu-item]')).map((b) =>
    b.textContent?.trim(),
  );

export const RESUMENES: catalogApi.ProductoAdminResumen[] = [
  {
    id: 7,
    codigo: 'LEG01',
    nombre: 'Legging Pro',
    slug: 'legging-pro',
    estado: 'BORRADOR',
    destacado: false,
    precioDesde: 59.9,
    cantidadVariantes: 6,
    stockDisponible: 20,
    imagenUrl: null,
  },
  {
    id: 8,
    codigo: 'POL01',
    nombre: 'Polo Algodón',
    slug: 'polo-algodon',
    estado: 'ACTIVO',
    destacado: true,
    precioDesde: 35,
    cantidadVariantes: 4,
    stockDisponible: 0,
    imagenUrl: null,
  },
];

/** Monta un componente suelto con sus entradas. */
export async function montarComp<T>(
  tipo: Type<T>,
  entradas: Record<string, unknown> = {},
  proveedores: (Provider | EnvironmentProviders)[] = [],
) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...proveedores] });
  const fixture = TestBed.createComponent(tipo);
  for (const [nombre, valor] of Object.entries(entradas))
    fixture.componentRef.setInput(nombre, valor);
  await asentar(fixture);
  return { fixture, el: fixture.nativeElement as HTMLElement, comp: fixture.componentInstance };
}

export const TALLAS: catalogApi.Talla[] = ['XS', 'S', 'M', 'L', 'XL'].map((codigo, i) => ({
  id: i + 1,
  codigo,
  orden: i + 1,
  activo: true,
}));
export const COLORES: catalogApi.Color[] = [
  { id: 1, nombre: 'Negro', hex: '#000000', activo: true },
  { id: 2, nombre: 'Azul', hex: '#1d4ed8', activo: true },
  { id: 3, nombre: 'Rojo', hex: '#dc2626', activo: true },
];
export const CATEGORIAS: catalogApi.CategoriaAdmin[] = [
  {
    id: 10,
    padreId: null,
    nombre: 'Leggings',
    slug: 'leggings',
    orden: 1,
    activo: true,
    cantidadProductos: 2,
    tieneGuiaTallas: true,
    hijas: [
      {
        id: 11,
        padreId: 10,
        nombre: 'Cortos',
        slug: 'cortos',
        orden: 1,
        activo: true,
        cantidadProductos: 1,
        tieneGuiaTallas: false,
      },
    ],
  },
];

export const PRODUCTO: catalogApi.ProductoAdmin = {
  id: 7,
  codigo: 'LEG01',
  nombre: 'Legging Pro',
  slug: 'legging-pro',
  descripcion: 'Tela de compresión.',
  precioBase: 59.9,
  estado: 'BORRADOR',
  destacado: false,
  ordenDestacado: null,
  modoDescuento: 'GLOBAL',
  tramosPropios: [],
  categoriaIds: [11],
  variantes: [
    {
      id: 101,
      sku: 'LEG01-NEG-S',
      tallaId: 2,
      talla: 'S',
      colorId: 1,
      color: 'Negro',
      precio: null,
      activo: true,
      fisico: 5,
      reservado: 0,
      disponible: 5,
    },
    {
      id: 102,
      sku: 'LEG01-AZU-S',
      tallaId: 2,
      talla: 'S',
      colorId: 2,
      color: 'Azul',
      precio: 64.9,
      activo: true,
      fisico: 0,
      reservado: 0,
      disponible: 0,
    },
  ],
  imagenes: [],
  version: 3,
};

export const MAESTROS_FORM = {
  '/admin/tallas': TALLAS,
  '/admin/colores': COLORES,
  '/admin/categorias': CATEGORIAS,
};
