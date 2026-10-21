import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import type { notificationApi } from 'shared';
import { montar } from '../../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';

vi.setConfig({ testTimeout: 30000 });

const LISTA = '/api/v1/admin/notificaciones';
const http = () => TestBed.inject(HttpTestingController);
const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });

function envio(id: string, cambios: Partial<notificationApi.EnvioResumen> = {}): notificationApi.EnvioResumen {
  return {
    id,
    plantilla: 'PEDIDO_CONFIRMADO',
    destinatario: 'ana@fitstore.demo',
    asunto: `Asunto ${id}`,
    referencia: 'FS-000124',
    estado: 'ENVIADO',
    intentos: 1,
    conAdjunto: true,
    creadoEn: '2026-10-22T15:00:00Z',
    enviadoEn: '2026-10-22T15:00:03Z',
    ...cambios,
  };
}
const ENVIADO = envio('e1');
const FALLIDO = envio('e2', { estado: 'FALLIDO', intentos: 5, enviadoEn: null, plantilla: 'EN_CAMINO' });
const PENDIENTE = envio('e3', { estado: 'PENDIENTE', intentos: 0, enviadoEn: null, referencia: 'RC-2026-000007' });
const sinReferencia = envio('e4', { referencia: null, plantilla: 'BIENVENIDA' });

const pagina = (contenido: notificationApi.EnvioResumen[]) => ({
  contenido,
  pagina: 0,
  tamano: 20,
  totalElementos: contenido.length,
  totalPaginas: 1,
  contadores: { PENDIENTE: 1, ENVIANDO: 0, ENVIADO: 2, FALLIDO: 1 },
});
const detalle = (e: notificationApi.EnvioResumen): notificationApi.Envio => ({
  ...e,
  historialIntentos:
    e.estado === 'FALLIDO'
      ? [
          { numero: 1, fecha: '2026-10-22T15:00:03Z', error: 'Connection refused' },
          { numero: 2, fecha: '2026-10-22T15:01:03Z', error: 'Connection refused' },
        ]
      : [{ numero: 1, fecha: '2026-10-22T15:00:03Z', error: null }],
  ultimoError: e.estado === 'FALLIDO' ? 'Connection refused' : null,
  proximoIntentoEn: null,
});

const pedidoLista = () => http().expectOne((r) => r.url === LISTA && r.method === 'GET');

/** Los diálogos y los paneles abren con animación: se espera la petición hasta 3 s de reloj real. */
async function esperar(estabilizar: () => Promise<void>, coincide: (r: { url: string; method: string }) => boolean) {
  const limite = Date.now() + 3000;
  while (Date.now() < limite) {
    const hallada = http().match(coincide);
    if (hallada.length) return hallada[0];
    await estabilizar();
    await new Promise((r) => setTimeout(r, 10));
  }
  return http().expectOne(coincide);
}

async function abrir(url = '/notificaciones') {
  const h = await montar(SESION_ADMIN, { url });
  const el = () => h.routeNativeElement as HTMLElement;
  // Con una petición en curso `whenStable` no termina (rxResource la cuenta como tarea pendiente): se refresca por reloj
  const estabilizar = async () => {
    for (let i = 0; i < 3; i++) {
      h.fixture.detectChanges();
      await new Promise((r) => setTimeout(r, 5));
    }
    h.fixture.detectChanges();
  };
  const cargar = async (lista = [ENVIADO, FALLIDO, PENDIENTE, sinReferencia]) => {
    pedidoLista().flush(ok(pagina(lista)));
    await estabilizar();
  };
  const filas = () => [...el().querySelectorAll<HTMLElement>('tr.mat-mdc-row, tr[mat-row]')];
  const fila = (texto: string) => filas().find((f) => f.textContent?.includes(texto))!;
  const accion = (texto: string, nombre: string) =>
    [...fila(texto).querySelectorAll<HTMLButtonElement>('.fs-acciones button')].find((b) =>
      b.textContent?.includes(nombre),
    );
  const overlay = () => TestBed.inject(OverlayContainer).getContainerElement();
  const botonDialogo = (texto: string) =>
    [...overlay().querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(texto))!;
  return { h, el, estabilizar, cargar, filas, fila, accion, overlay, botonDialogo };
}

describe('Registro de envíos (P-7-01)', () => {
  it('CA-7-19 lista los envíos con fecha, asunto, plantilla, destinatario, referencia, estado e intentos', async () => {
    const p = await abrir();
    await p.cargar();

    expect(p.el().querySelector('h1')?.textContent).toContain('Notificaciones');
    expect(p.filas()).toHaveLength(4);
    const primera = p.fila('Asunto e1').textContent ?? '';
    expect(primera).toContain('22/10/2026');
    expect(primera).toContain('Pedido confirmado');
    expect(primera).toContain('ana@fitstore.demo');
    expect(primera).toContain('FS-000124');
    expect(primera).toContain('Enviado');
    expect(p.fila('Asunto e2').textContent).toContain('Fallido');
    expect(p.fila('Asunto e3').textContent).toContain('Pendiente');
  });

  it('CA-7-19 la referencia enlaza al pedido o al reclamo y la fila sin referencia muestra un guion', async () => {
    const p = await abrir();
    await p.cargar();

    const pedido = p.fila('Asunto e1').querySelector<HTMLAnchorElement>('a[href*="pedidos"]')!;
    expect(pedido.getAttribute('href')).toContain('/pedidos?q=FS-000124');
    const reclamo = p.fila('Asunto e3').querySelector<HTMLAnchorElement>('a[href*="reclamos"]')!;
    expect(reclamo.getAttribute('href')).toContain('/reclamos?q=RC-2026-000007');
    expect(p.fila('Asunto e4').textContent).toContain('—');
  });

  it('CA-7-19 los filtros de la URL (estado, plantilla, destinatario, referencia y fechas) se envían a la API', async () => {
    const p = await abrir(
      '/notificaciones?estado=FALLIDO&plantilla=EN_CAMINO&q=ana&referencia=FS-000124&desde=2026-10-01&hasta=2026-10-31',
    );
    const peticion = pedidoLista();
    expect(peticion.request.params.get('estado')).toBe('FALLIDO');
    expect(peticion.request.params.get('plantilla')).toBe('EN_CAMINO');
    expect(peticion.request.params.get('destinatario')).toBe('ana');
    expect(peticion.request.params.get('referencia')).toBe('FS-000124');
    expect(peticion.request.params.get('desde')).toBe('2026-10-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
    peticion.flush(ok(pagina([FALLIDO])));
    await p.estabilizar();
    expect(p.filas()).toHaveLength(1);
  });

  it('CA-7-19 los chips de estado muestran el contador de la API', async () => {
    const p = await abrir();
    await p.cargar();

    const chips = p.el().textContent ?? '';
    expect(chips).toContain('Fallido');
    expect(chips).toContain('Enviado');
  });

  it('CA-7-19 sin resultados muestra el estado vacío', async () => {
    const p = await abrir();
    await p.cargar([]);

    expect(p.el().textContent).toContain('No hay correos con esos filtros');
  });

  it('CA-7-19 si falla la carga ofrece "Reintentar"', async () => {
    const q = await abrir();
    pedidoLista().flush(null, { status: 500, statusText: 'Error' });
    await q.estabilizar();

    expect(
      [...q.el().querySelectorAll<HTMLButtonElement>('button')].some((b) => b.textContent?.includes('Reintentar')),
    ).toBe(true);
  });

  it('CA-7-20 el asunto abre el panel lateral con el destinatario, cada intento y el último error', async () => {
    const p = await abrir();
    await p.cargar();

    p.fila('Asunto e2').querySelector<HTMLButtonElement>('button.fs-enlace')!.click();
    await p.estabilizar();
    http()
      .expectOne((r) => r.url === `${LISTA}/e2` && r.method === 'GET')
      .flush(ok(detalle(FALLIDO)));
    await p.estabilizar();

    const panel = p.el().querySelector('fs-panel-detalle-envio')!;
    const texto = panel.textContent ?? '';
    expect(texto).toContain('Asunto e2');
    expect(texto).toContain('ana@fitstore.demo');
    expect(texto).toContain('FS-000124');
    expect(texto).toContain('Último error: Connection refused');
    expect(texto).toContain('Intento 1');
    expect(texto).toContain('Intento 2');
    expect(texto).toContain('22/10/2026');

    panel.querySelector<HTMLButtonElement>('button[aria-label="Cerrar el detalle"]')!.click();
    await p.estabilizar();
    expect(p.el().querySelector('fs-panel-detalle-envio')).toBeNull();
  });

  it('CA-7-21 "Reenviar" solo aparece en los FALLIDO y ENVIADO, pide confirmación y vuelve a cargar la lista', async () => {
    const p = await abrir();
    await p.cargar();
    expect(p.accion('Asunto e1', 'Reenviar')).toBeDefined();
    expect(p.accion('Asunto e2', 'Reenviar')).toBeDefined();
    expect(p.accion('Asunto e3', 'Reenviar')).toBeUndefined();

    p.accion('Asunto e2', 'Reenviar')!.click();
    await p.estabilizar();
    expect(p.overlay().textContent).toContain('¿Reenviar este correo?');
    http().expectNone((r) => r.url === `${LISTA}/e2/reenviar`);
    p.botonDialogo('Reenviar').click();
    await p.estabilizar();

    const peticion = await esperar(p.estabilizar, (r) => r.url === `${LISTA}/e2/reenviar` && r.method === 'POST');
    peticion.flush(ok(detalle({ ...FALLIDO, estado: 'PENDIENTE' })));
    await p.estabilizar();
    const recarga = await esperar(p.estabilizar, (r) => r.url === LISTA && r.method === 'GET');
    recarga.flush(ok(pagina([ENVIADO, { ...FALLIDO, estado: 'PENDIENTE' }, PENDIENTE, sinReferencia])));
    await p.estabilizar();

    expect(p.fila('Asunto e2').textContent).toContain('Pendiente');
  });

  it('CA-7-21 si se vuelve atrás en la confirmación no se reenvía nada', async () => {
    const p = await abrir();
    await p.cargar();

    p.accion('Asunto e1', 'Reenviar')!.click();
    await p.estabilizar();
    p.botonDialogo('Volver').click();
    await p.estabilizar();
    await new Promise((r) => setTimeout(r, 300));

    http().expectNone((r) => r.url.endsWith('/reenviar'));
  });

  it('CA-7-22 un OPERADOR que entra por URL a /notificaciones ve la página 403 y no se pide nada a la API', async () => {
    const h = await montar(SESION_OPERADOR, { url: '/notificaciones' });

    expect(TestBed.inject(Router).url).toBe('/403');
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('No tienes permiso');
    http().expectNone((r) => r.url === LISTA);
  });

  it('la ruta /notificaciones/plantillas (extensión de alcance) sigue mostrando "Próximamente"', async () => {
    const p = await abrir('/notificaciones/plantillas');
    await p.estabilizar();

    expect(p.el().textContent).toContain('Próximamente');
    http().expectNone((r) => r.url === LISTA);
  });
});
