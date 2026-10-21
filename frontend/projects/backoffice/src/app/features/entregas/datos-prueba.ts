import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, type Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SesionStore, provideFitstoreHttp, provideSesionConfig, type deliveryApi } from 'shared';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';

/** Datos y ayudas de prueba de `features/entregas` y `features/maestros-envio` (no forman parte de la aplicación). */
export const respuesta = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

export const REPARTIDOR: deliveryApi.Repartidor = {
  id: 101,
  nombres: 'Juan Pérez',
  telefono: '987654321',
  vehiculo: 'MOTO',
  placa: 'ABC-123',
  activo: true,
  tieneEntregas: false,
};

export function resumen(
  id: string,
  cambios: Partial<deliveryApi.EntregaResumen> = {},
): deliveryApi.EntregaResumen {
  return {
    id,
    pedidoId: `pedido-${id}`,
    codigoPedido: `FS-${id.padStart(6, '0')}`,
    modalidad: 'DELIVERY_LOCAL',
    estado: 'PENDIENTE_ASIGNACION',
    destinatarioNombre: 'Lucía Mendoza',
    distrito: 'Miraflores',
    fechaProgramada: null,
    repartidorNombre: null,
    agenciaNombre: null,
    intento: 0,
    siguienteAccion: 'ASIGNAR',
    ...cambios,
  };
}

export function entrega(
  id: string,
  cambios: Partial<deliveryApi.Entrega> = {},
): deliveryApi.Entrega {
  return {
    ...resumen(id),
    destinatario: {
      nombres: 'Lucía',
      apellidos: 'Mendoza',
      dni: '45678912',
      telefono: '955111222',
    },
    direccion: {
      ubigeo: '150122',
      departamento: 'Lima',
      provincia: 'Lima',
      distrito: 'Miraflores',
      direccion: 'Av. Larco 345 dpto 201',
      referencia: 'Frente al parque Kennedy',
    },
    resumenItems: 'Legging Pro Compresión M Negro × 2',
    bultos: 2,
    repartidor: null,
    agenciaId: null,
    codigoSeguimiento: null,
    claveRecojo: null,
    urlSeguimiento: null,
    eventos: [
      {
        estado: 'PENDIENTE_ASIGNACION',
        observacion: null,
        actorNombre: null,
        intento: 0,
        fecha: '2026-10-19T15:00:00Z',
      },
    ],
    accionesPermitidas: ['ASIGNAR'],
    version: 0,
    ...cambios,
  } as deliveryApi.Entrega;
}

export function pagina(
  contenido: deliveryApi.EntregaResumen[],
  contadores: Record<string, number> = {},
) {
  return {
    contenido,
    pagina: 0,
    tamano: 20,
    totalElementos: contenido.length,
    totalPaginas: 1,
    contadores,
  };
}

/** Arma un arreglo de rutas bajo `prefijo` con HTTP simulado y la sesión de un ADMIN o un OPERADOR. */
export async function abrirRuta(
  prefijo: string,
  rutasHijas: Routes,
  url: string,
  rol: 'ADMIN' | 'OPERADOR' = 'OPERADOR',
) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: prefijo, children: rutasHijas }]),
      provideFitstoreHttp({ carrito: false }),
      provideHttpClientTesting(),
      provideSesionConfig({ tipo: 'STAFF', rutaLogin: '/ingresar', rutaSinPermiso: '/403' }),
    ],
  });
  TestBed.inject(SesionStore).iniciarSesion(rol === 'ADMIN' ? SESION_ADMIN : SESION_OPERADOR);
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const el = () => harness.routeNativeElement as HTMLElement;
  const ctrl = TestBed.inject(HttpTestingController);
  const estabilizar = async () => {
    harness.detectChanges();
    await harness.fixture.whenStable();
    await new Promise((r) => setTimeout(r, 0));
    harness.detectChanges();
  };
  const panel = () => TestBed.inject(OverlayContainer).getContainerElement();
  const botonPanel = (texto: string) =>
    [...panel().querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  /** Espera (con la animación de los diálogos) a que salga la petición que cumple la condición. */
  // Hasta 3 s de reloj real: con la suite completa en paralelo, unos pocos ciclos no siempre alcanzan.
  const pausa = () => new Promise((r) => setTimeout(r, 10));
  const esperar = async (coincide: (r: { url: string; method: string }) => boolean) => {
    const limite = Date.now() + 3000;
    while (Date.now() < limite) {
      const hallada = ctrl.match(coincide);
      if (hallada.length) return hallada[0];
      await estabilizar();
      await pausa();
    }
    return ctrl.expectOne(coincide);
  };
  /** Estabiliza la vista hasta que se cumpla la condición (el cierre de un diálogo termina con una animación). */
  const hasta = async (condicion: () => boolean) => {
    const limite = Date.now() + 3000;
    while (Date.now() < limite && !condicion()) {
      await estabilizar();
      await pausa();
    }
  };
  return { harness, el, ctrl, estabilizar, panel, botonPanel, esperar, hasta };
}

export const filasTabla = (el: HTMLElement) => [
  ...el.querySelectorAll<HTMLElement>('tr.mat-mdc-row, tr[mat-row]'),
];

export const botonDe = (contenedor: ParentNode, texto: string) =>
  [...contenedor.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
    b.textContent?.trim().includes(texto),
  )!;
