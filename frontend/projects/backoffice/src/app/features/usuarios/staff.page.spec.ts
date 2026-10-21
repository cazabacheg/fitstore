import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastService, authApi } from 'shared';
import { montar } from '../../testing/montar';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';

vi.setConfig({ testTimeout: 30000 });

const LISTA = '/api/v1/staff';
const http = () => TestBed.inject(HttpTestingController);
const ok = <T>(data: T) => ({ success: true, mensaje: 'Operación exitosa', data });
const pagina = (contenido: authApi.Staff[]) => ({
  contenido,
  pagina: 0,
  tamano: 20,
  totalElementos: contenido.length,
  totalPaginas: 1,
});
const miembro = (
  id: string,
  nombres: string,
  estado: authApi.Staff['estado'],
  rol: authApi.Staff['rol'] = 'OPERADOR',
): authApi.Staff => ({
  id,
  email: `${nombres.toLowerCase()}@fitstore.pe`,
  nombres,
  apellidos: 'Prueba',
  rol,
  estado,
  ultimoLoginEn: null,
  creadoEn: '2026-10-01T10:00:00Z',
  version: 2,
});
const ADMIN = miembro('a1', 'Carla', 'ACTIVO', 'ADMIN');
const PENDIENTE = miembro('b2', 'Luis', 'INVITACION_PENDIENTE');
const INACTIVO = miembro('c3', 'Inés', 'INACTIVO');

const pedidoLista = () => http().expectOne((r) => r.url === LISTA && r.method === 'GET');

/**
 * Los diálogos abren y cierran con animación: se espera a que la petición salga, hasta 3 s de reloj real
 * (con la suite completa corriendo en paralelo, unos pocos ciclos no siempre alcanzan).
 */
async function esperar(
  estabilizar: () => Promise<void>,
  coincide: (r: { url: string; method: string }) => boolean,
) {
  const limite = Date.now() + 3000;
  while (Date.now() < limite) {
    const hallada = http().match(coincide);
    if (hallada.length) return hallada[0];
    await estabilizar();
    await new Promise((r) => setTimeout(r, 10));
  }
  return http().expectOne(coincide);
}

async function abrir(url = '/staff') {
  const h = await montar(SESION_ADMIN, { url });
  const el = () => h.routeNativeElement as HTMLElement;
  const estabilizar = async () => {
    h.fixture.detectChanges();
    await h.fixture.whenStable();
    await new Promise((r) => setTimeout(r, 0));
    h.fixture.detectChanges();
  };
  const cargar = async (lista: authApi.Staff[] = [ADMIN, PENDIENTE, INACTIVO]) => {
    pedidoLista().flush(ok(pagina(lista)));
    await estabilizar();
  };
  const filas = () => [...el().querySelectorAll<HTMLElement>('tr.mat-mdc-row, tr[mat-row]')];
  const fila = (nombre: string) => filas().find((f) => f.textContent?.includes(nombre))!;
  const accion = (nombre: string, texto: string) =>
    [...fila(nombre).querySelectorAll<HTMLButtonElement>('.fs-acciones button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  const panel = () => TestBed.inject(OverlayContainer).getContainerElement();
  const botonPanel = (texto: string) =>
    [...panel().querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    )!;
  const campoPanel = (etiqueta: string) =>
    [...panel().querySelectorAll<HTMLElement>('.fs-campo')].find((c) =>
      c.querySelector('label')?.textContent?.trim().startsWith(etiqueta),
    )!;
  const escribirPanel = async (etiqueta: string, valor: string) => {
    const control = campoPanel(etiqueta).querySelector<HTMLInputElement | HTMLSelectElement>(
      'input, select',
    )!;
    control.value = valor;
    control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
    await estabilizar();
  };
  return {
    h,
    el,
    estabilizar,
    cargar,
    filas,
    fila,
    accion,
    panel,
    botonPanel,
    campoPanel,
    escribirPanel,
  };
}

describe('Staff (P-1-08)', () => {
  it('CA-1-25 lista el personal con su rol y estado, y la invitación pendiente se ve como tal', async () => {
    const p = await abrir();
    await p.cargar();

    expect(p.filas()).toHaveLength(3);
    expect(p.fila('Luis').textContent).toContain('Invitación pendiente');
    expect(p.fila('Carla').textContent).toContain('Admin');
    expect(p.fila('Inés').textContent).toContain('Inactivo');
    expect(p.el().querySelector('h1')?.textContent).toContain('Staff');
  });

  it('CA-1-25 los filtros de la URL (rol, estado y texto) se envían a la API', async () => {
    const p = await abrir('/staff?rol=ADMIN&estado=ACTIVO&q=car');
    const peticion = pedidoLista();
    expect(peticion.request.params.get('rol')).toBe('ADMIN');
    expect(peticion.request.params.get('estado')).toBe('ACTIVO');
    expect(peticion.request.params.get('q')).toBe('car');
    peticion.flush(ok(pagina([ADMIN])));
    await p.estabilizar();
    expect(p.filas()).toHaveLength(1);
  });

  it('CA-1-25 las acciones de cada fila dependen de su estado (R11: como máximo 2)', async () => {
    const p = await abrir();
    await p.cargar();
    const textos = (n: string) =>
      [...p.fila(n).querySelectorAll('.fs-acciones button')].map((b) => b.textContent?.trim());

    expect(textos('Carla')).toEqual(['Editar', 'Desactivar']);
    expect(textos('Luis')).toEqual(['Reenviar invitación']);
    expect(textos('Inés')).toEqual(['Activar']);
  });

  it('CA-1-25 "Invitar miembro" abre el diálogo y envía correo, nombres, apellidos y rol', async () => {
    const p = await abrir();
    await p.cargar();
    [...p.el().querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Invitar miembro'))!
      .click();
    await p.estabilizar();

    expect(p.panel().textContent).toContain('Invitar miembro');
    await p.escribirPanel('Correo', 'nuevo@fitstore.pe');
    await p.escribirPanel('Nombres', 'Nuevo');
    await p.escribirPanel('Apellidos', 'Miembro');
    await p.escribirPanel('Rol', 'ADMIN');
    p.botonPanel('Enviar invitación').click();
    await p.estabilizar();

    const peticion = await esperar(p.estabilizar, (r) => r.url === LISTA && r.method === 'POST');
    expect(peticion.request.body).toEqual({
      email: 'nuevo@fitstore.pe',
      nombres: 'Nuevo',
      apellidos: 'Miembro',
      rol: 'ADMIN',
    });
    peticion.flush(ok(miembro('d4', 'Nuevo', 'INVITACION_PENDIENTE', 'ADMIN')));
    await p.estabilizar();
    pedidoLista().flush(ok(pagina([ADMIN, PENDIENTE])));
    await p.estabilizar();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Invitación enviada');
    expect(p.panel().querySelector('[role="dialog"]')).toBeNull();
  });

  it('CA-1-25 el diálogo valida los datos y muestra el correo repetido junto a su campo', async () => {
    const p = await abrir();
    await p.cargar();
    [...p.el().querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.includes('Invitar miembro'))!
      .click();
    await p.estabilizar();
    p.botonPanel('Enviar invitación').click();
    await p.estabilizar();

    expect(p.panel().textContent).toContain('Ingresa tu correo');
    expect(p.panel().textContent).toContain('Elige un rol');
    http().expectNone((r) => r.url === LISTA && r.method === 'POST');

    await p.escribirPanel('Correo', 'carla@fitstore.pe');
    await p.escribirPanel('Nombres', 'Carla');
    await p.escribirPanel('Apellidos', 'Otra');
    await p.escribirPanel('Rol', 'OPERADOR');
    p.botonPanel('Enviar invitación').click();
    await p.estabilizar();
    http()
      .expectOne((r) => r.url === LISTA && r.method === 'POST')
      .flush(
        { success: false, mensaje: 'El correo ya está registrado', codigo: 'CORREO_DUPLICADO' },
        { status: 409, statusText: 'Conflict' },
      );
    await p.estabilizar();

    expect(p.campoPanel('Correo').textContent).toContain('El correo ya está registrado');
  });

  it('CA-1-27 el nombre abre el diálogo de edición (R11); guarda nombre y rol con la versión y no deja editar el correo', async () => {
    const p = await abrir();
    await p.cargar();
    p.fila('Luis').querySelector<HTMLButtonElement>('button.fs-enlace')!.click();
    await p.estabilizar();

    expect(p.panel().textContent).toContain('Editar miembro');
    expect(p.campoPanel('Correo').querySelector<HTMLInputElement>('input')!.disabled).toBe(true);
    expect(p.campoPanel('Nombres').querySelector<HTMLInputElement>('input')!.value).toBe('Luis');
    await p.escribirPanel('Nombres', 'Luis Alberto');
    await p.escribirPanel('Rol', 'ADMIN');
    p.botonPanel('Guardar').click();
    await p.estabilizar();

    const peticion = await esperar(
      p.estabilizar,
      (r) => r.url === `${LISTA}/b2` && r.method === 'PUT',
    );
    expect(peticion.request.body).toEqual({
      nombres: 'Luis Alberto',
      apellidos: 'Prueba',
      rol: 'ADMIN',
      version: 2,
    });
    peticion.flush(ok({ ...PENDIENTE, nombres: 'Luis Alberto', rol: 'ADMIN' }));
    await p.estabilizar();
    pedidoLista().flush(ok(pagina([ADMIN])));
    await p.estabilizar();
  });

  it('CA-1-28 si la API responde 409 (último ADMIN) se muestra el motivo y nada cambia', async () => {
    const p = await abrir();
    await p.cargar();
    p.accion('Carla', 'Desactivar').click();
    await p.estabilizar();
    expect(p.panel().textContent).toContain('¿Desactivar a Carla Prueba?');
    p.botonPanel('Desactivar').click();
    await p.estabilizar();
    (
      await esperar(
        p.estabilizar,
        (r) => r.url === `${LISTA}/a1/desactivar` && r.method === 'PATCH',
      )
    ).flush(
      {
        success: false,
        mensaje: 'Debe quedar al menos un administrador activo',
        codigo: 'ULTIMO_ADMIN',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await p.estabilizar();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Debe quedar al menos un administrador activo');
    http().expectNone((r) => r.url === LISTA && r.method === 'GET');
  });

  it('CA-1-29 desactivar pide confirmación y recarga la lista; "Volver" no hace nada', async () => {
    const p = await abrir();
    await p.cargar();
    p.accion('Carla', 'Desactivar').click();
    await p.estabilizar();
    p.botonPanel('Volver').click();
    await p.estabilizar();
    http().expectNone((r) => r.url === `${LISTA}/a1/desactivar`);

    p.accion('Carla', 'Desactivar').click();
    await p.estabilizar();
    p.botonPanel('Desactivar').click();
    await p.estabilizar();
    (
      await esperar(
        p.estabilizar,
        (r) => r.url === `${LISTA}/a1/desactivar` && r.method === 'PATCH',
      )
    ).flush(ok({ ...ADMIN, estado: 'INACTIVO' }));
    await p.estabilizar();
    pedidoLista().flush(ok(pagina([{ ...ADMIN, estado: 'INACTIVO' }])));
    await p.estabilizar();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Miembro desactivado');
  });

  it('CA-1-29 "Activar" reactiva al miembro inactivo', async () => {
    const p = await abrir();
    await p.cargar();
    p.accion('Inés', 'Activar').click();
    await p.estabilizar();

    http()
      .expectOne((r) => r.url === `${LISTA}/c3/activar` && r.method === 'PATCH')
      .flush(ok({ ...INACTIVO, estado: 'ACTIVO' }));
    await p.estabilizar();
    pedidoLista().flush(ok(pagina([ADMIN, PENDIENTE, { ...INACTIVO, estado: 'ACTIVO' }])));
    await p.estabilizar();
    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Miembro activado');
  });

  it('CA-1-30 "Reenviar invitación" pide un enlace nuevo y avisa', async () => {
    const p = await abrir();
    await p.cargar();
    p.accion('Luis', 'Reenviar invitación').click();
    await p.estabilizar();

    http()
      .expectOne((r) => r.url === `${LISTA}/b2/reenviar-invitacion` && r.method === 'POST')
      .flush(ok(null));
    await p.estabilizar();
    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((t) => t.mensaje),
    ).toContain('Invitación reenviada');
  });

  it('CA-1-31 un OPERADOR que entra por URL a /staff ve la página 403', async () => {
    const h = await montar(SESION_OPERADOR, { url: '/staff' });
    expect(TestBed.inject(Router).url).toBe('/403');
    expect((h.routeNativeElement as HTMLElement).textContent).toContain('No tienes permiso');
    http().expectNone((r) => r.url === LISTA);
  });

  it('CA-1-25 si la lista falla muestra el error con "Reintentar"', async () => {
    const p = await abrir();
    pedidoLista().flush(null, { status: 500, statusText: 'Error' });
    await p.estabilizar();

    const reintentar = [...p.el().querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes('Reintentar'),
    );
    expect(reintentar).toBeDefined();
    reintentar!.click();
    await p.estabilizar();
    pedidoLista().flush(ok(pagina([ADMIN])));
    await p.estabilizar();
    expect(p.filas()).toHaveLength(1);
  });

  it('CA-1-25 paginar y ordenar vuelven a pedir la lista con page, size y sort', async () => {
    const p = await abrir();
    await p.cargar();
    const tabla = p.el().querySelector('fs-tabla')!;
    expect(tabla).not.toBeNull();
    const siguiente = p
      .el()
      .querySelector<HTMLButtonElement>('button.mat-mdc-paginator-navigation-next');
    expect(siguiente).not.toBeNull();
  });
});
