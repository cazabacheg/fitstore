import { HttpTestingController } from '@angular/common/http/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastService, authApi } from 'shared';
import { montar } from '../../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';

const LISTA = '/api/v1/clientes';
const http = () => TestBed.inject(HttpTestingController);
const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });
const resumen = (id: string, nombres: string, activo = true): authApi.ClienteResumen => ({
  id,
  email: `${nombres.toLowerCase()}@correo.pe`,
  nombres,
  apellidos: 'Quispe',
  dni: '4567891' + id.length,
  telefono: '987654321',
  activo,
  creadoEn: '2026-10-01T10:00:00Z',
});
const detalle = (activo = true): authApi.ClienteDetalle => ({
  ...resumen('c1', 'Rosa', activo),
  ultimoLoginEn: '2026-10-19T09:00:00Z',
  direcciones: [
    { id: 1, alias: 'Casa', ubigeo: '150115', direccion: 'Jr. Gamarra 123', referencia: 'Frente al parque', telefonoContacto: null, predeterminada: true },
    { id: 2, alias: 'Trabajo', ubigeo: '070101', direccion: 'Av. Sáenz Peña 780', referencia: null, telefonoContacto: '912345678', predeterminada: false },
  ],
});
const pagina = (contenido: authApi.ClienteResumen[]) => ({ contenido, pagina: 0, tamano: 20, totalElementos: contenido.length, totalPaginas: 1 });

async function abrir(sesion: typeof SESION_ADMIN, url: string) {
  const h = await montar(sesion, { url });
  const el = () => h.routeNativeElement as HTMLElement;
  const estabilizar = async () => {
    h.fixture.detectChanges();
    await h.fixture.whenStable();
    await new Promise((r) => setTimeout(r, 0));
    h.fixture.detectChanges();
  };
  const panel = () => TestBed.inject(OverlayContainer).getContainerElement();
  const boton = (texto: string) =>
    [...el().querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.trim() === texto);
  return { h, el, estabilizar, panel, boton };
}

describe('Clientes (P-1-09)', () => {
  it('CA-1-32 lista los clientes; el nombre es el enlace al detalle y la fila lleva "Ver ›" (R11)', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes');
    http().expectOne((r) => r.url === LISTA).flush(ok(pagina([resumen('c1', 'Rosa'), resumen('c2', 'Beto', false)])));
    await p.estabilizar();

    const enlace = p.el().querySelector<HTMLAnchorElement>('a.fs-codigo')!;
    expect(enlace.getAttribute('href')).toBe('/clientes/c1');
    expect(p.el().textContent).toContain('rosa@correo.pe');
    expect(p.el().textContent).toContain('Bloqueado');
    expect(p.el().textContent).toContain('Ver ›');
  });

  it('CA-1-32 busca por nombre, correo o DNI y filtra por estado con los parámetros de la URL', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes?q=quispe&activo=false');
    const peticion = http().expectOne((r) => r.url === LISTA);
    expect(peticion.request.params.get('q')).toBe('quispe');
    expect(peticion.request.params.get('activo')).toBe('false');
    peticion.flush(ok(pagina([])));
    await p.estabilizar();

    expect(p.el().textContent).toContain('Sin resultados');
  });

  it('CA-1-32 si la lista falla muestra el error con "Reintentar"', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes');
    http().expectOne((r) => r.url === LISTA).flush(null, { status: 500, statusText: 'Error' });
    await p.estabilizar();

    p.boton('Reintentar')!.click();
    http().expectOne((r) => r.url === LISTA).flush(ok(pagina([resumen('c1', 'Rosa')])));
    await p.estabilizar();
    expect(p.el().textContent).toContain('rosa@correo.pe');
  });

  it('CA-1-33 el detalle muestra los datos, las direcciones y el enlace "Ver sus pedidos"', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes/c1');
    http().expectOne((r) => r.url === `${LISTA}/c1`).flush(ok(detalle()));
    await p.estabilizar();

    expect(p.el().textContent).toContain('Rosa Quispe');
    expect(p.el().textContent).toContain('rosa@correo.pe');
    expect(p.el().textContent).toContain('987654321');
    expect(p.el().textContent).toContain('Casa');
    expect(p.el().textContent).toContain('Predeterminada');
    expect(p.el().textContent).toContain('Av. Sáenz Peña 780');
    const pedidos = [...p.el().querySelectorAll<HTMLAnchorElement>('a')].find((a) => a.textContent?.includes('Ver sus pedidos'))!;
    expect(pedidos.getAttribute('href')).toBe('/pedidos?clienteId=c1');
  });

  it('CA-1-33 un cliente que no existe (404) muestra el error', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes/nada');
    http().expectOne((r) => r.url === `${LISTA}/nada`).flush(
      { success: false, mensaje: 'No encontramos lo que buscas', codigo: 'NO_ENCONTRADO' },
      { status: 404, statusText: 'Not Found' },
    );
    await p.estabilizar();

    expect(p.el().querySelector('[role="alert"]')).not.toBeNull();
  });

  it('CA-1-34 un OPERADOR no ve el botón "Bloquear"', async () => {
    const p = await abrir(SESION_OPERADOR, '/clientes/c1');
    http().expectOne((r) => r.url === `${LISTA}/c1`).flush(ok(detalle()));
    await p.estabilizar();

    expect(p.boton('Bloquear')).toBeUndefined();
    expect(p.boton('Desbloquear')).toBeUndefined();
  });

  it('CA-1-34 un ADMIN bloquea al cliente tras confirmar y el detalle pasa a "Bloqueado"', async () => {
    const p = await abrir(SESION_ADMIN, '/clientes/c1');
    http().expectOne((r) => r.url === `${LISTA}/c1`).flush(ok(detalle()));
    await p.estabilizar();
    p.boton('Bloquear')!.click();
    await p.estabilizar();

    expect(p.panel().textContent).toContain('¿Bloquear a Rosa Quispe?');
    [...p.panel().querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Bloquear cliente'))!.click();
    await p.estabilizar();
    const peticion = http().expectOne((r) => r.url === `${LISTA}/c1/desactivar`);
    expect(peticion.request.method).toBe('PATCH');
    peticion.flush(ok(detalle(false)));
    await p.estabilizar();

    expect(TestBed.inject(ToastService).toasts().map((t) => t.mensaje)).toContain('Cliente bloqueado');
    expect(p.el().textContent).toContain('Bloqueado');
    expect(p.boton('Desbloquear')).toBeDefined();
  });

  it('CA-1-34 un ADMIN desbloquea a un cliente bloqueado', async () => {
    const p = await abrir(SESION_ADMIN, '/clientes/c1');
    http().expectOne((r) => r.url === `${LISTA}/c1`).flush(ok(detalle(false)));
    await p.estabilizar();
    p.boton('Desbloquear')!.click();
    await p.estabilizar();
    [...p.panel().querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Desbloquear cliente'))!.click();
    await p.estabilizar();

    const peticion = http().expectOne((r) => r.url === `${LISTA}/c1/activar`);
    peticion.flush(ok(detalle(true)));
    await p.estabilizar();
    expect(TestBed.inject(ToastService).toasts().map((t) => t.mensaje)).toContain('Cliente desbloqueado');
    expect(p.boton('Bloquear')).toBeDefined();
  });

  it('CA-1-34 "Volver" en la confirmación no bloquea a nadie', async () => {
    const p = await abrir(SESION_ADMIN, '/clientes/c1');
    http().expectOne((r) => r.url === `${LISTA}/c1`).flush(ok(detalle()));
    await p.estabilizar();
    p.boton('Bloquear')!.click();
    await p.estabilizar();
    [...p.panel().querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Volver'))!.click();
    await p.estabilizar();

    http().expectNone((r) => r.url.endsWith('/desactivar'));
    expect(TestBed.inject(Router).url).toBe('/clientes/c1');
  });
});
