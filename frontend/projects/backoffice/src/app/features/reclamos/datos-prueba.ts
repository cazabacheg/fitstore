import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, type Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig, type settingsApi } from 'shared';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { RECLAMOS_ROUTES } from './reclamos.routes';

/** Datos y ayudas de prueba de `features/reclamos` (no forman parte de la aplicación). */
export const respuesta = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

export function resumen(
  id: number,
  cambios: Partial<settingsApi.ReclamoResumen> = {},
): settingsApi.ReclamoResumen {
  return {
    id,
    numero: `RC-2026-${String(id).padStart(6, '0')}`,
    tipo: 'RECLAMO',
    estado: 'REGISTRADO',
    consumidorNombre: 'Rosa Díaz',
    creadoEn: '2026-10-19T15:20:00Z',
    fechaLimite: '2026-11-09',
    porVencer: false,
    vencido: false,
    ...cambios,
  };
}

export function reclamo(
  id: number,
  cambios: Partial<settingsApi.Reclamo> = {},
): settingsApi.Reclamo {
  return {
    ...resumen(id),
    consumidor: {
      nombres: 'Rosa',
      apellidos: 'Díaz',
      tipoDocumento: 'DNI',
      numeroDocumento: '87654321',
      domicilio: 'Calle Mercaderes 456, Cercado, Arequipa',
      telefono: '912345678',
      email: 'rosa.diaz@correo.com',
      esMenor: false,
      apoderadoNombre: null,
    },
    bien: {
      tipo: 'PRODUCTO',
      descripcion: 'Casaca Wind talla M negra',
      montoReclamado: 89.9,
      pedidoCodigo: 'FS-000118',
    },
    detalle: 'La casaca llegó con el cierre roto.',
    pedidoConsumidor: 'Cambio por una nueva o devolución del dinero.',
    respuesta: null,
    respondidoEn: null,
    respondidoPor: null,
    ...cambios,
  };
}

export function pagina(contenido: settingsApi.ReclamoResumen[], extra = {}) {
  return {
    contenido,
    pagina: 0,
    tamano: 20,
    totalElementos: contenido.length,
    totalPaginas: 1,
    ...extra,
  };
}

/** Arma las rutas de reclamos con HTTP simulado y la sesión de un ADMIN o un OPERADOR. */
export async function abrirRuta(
  url: string,
  rol: 'ADMIN' | 'OPERADOR' = 'ADMIN',
  proveedores: unknown[] = [],
) {
  const rutas: Routes = [{ path: 'reclamos', children: RECLAMOS_ROUTES }];
  TestBed.configureTestingModule({
    providers: [
      provideRouter(rutas),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
      ...(proveedores as never[]),
    ],
  });
  TestBed.inject(SesionStore).iniciarSesion(rol === 'ADMIN' ? SESION_ADMIN : SESION_OPERADOR);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const estabilizar = async () => {
    harness.detectChanges();
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { harness, el: harness.routeNativeElement as HTMLElement, estabilizar };
}
