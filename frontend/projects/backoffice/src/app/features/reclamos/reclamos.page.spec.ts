import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { abrirRuta, pagina, respuesta, resumen } from './datos-prueba';

const LISTA = '/api/v1/admin/reclamos';
const FILAS = [
  resumen(15, { vencido: true, fechaLimite: '2026-10-16' }),
  resumen(16, { porVencer: true, fechaLimite: '2026-10-22', tipo: 'QUEJA' }),
  resumen(17, { estado: 'RESPONDIDO', fechaLimite: '2026-11-09' }),
];

async function abrir(url = '/reclamos', rol: 'ADMIN' | 'OPERADOR' = 'ADMIN') {
  const montaje = await abrirRuta(url, rol);
  const ctrl = TestBed.inject(HttpTestingController);
  return { ...montaje, ctrl };
}

const peticion = (ctrl: HttpTestingController) => ctrl.expectOne((r) => r.url === LISTA);
const filas = (el: HTMLElement) => [...el.querySelectorAll('tr.mat-mdc-row')] as HTMLElement[];

describe('ReclamosPage (P-3-06)', () => {
  it('CA-3-28 la bandeja pide la primera página y muestra los reclamos con número, consumidor, tipo, estado y plazo', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const p = peticion(ctrl);
    expect(p.request.params.get('page')).toBe('0');
    expect(p.request.params.get('size')).toBe('20');
    p.flush(respuesta(pagina(FILAS)));
    await estabilizar();

    expect(filas(el).length).toBe(3);
    expect(filas(el)[0].textContent).toContain('RC-2026-000015');
    expect(filas(el)[0].textContent).toContain('Rosa Díaz');
    expect(filas(el)[1].textContent).toContain('Queja');
    expect(filas(el)[2].textContent).toContain('Respondido');
    expect(filas(el)[0].textContent).toContain('16/10/2026');
  });

  it('CA-3-28 el número del reclamo es un enlace al detalle y cada fila lleva "Ver ›" (R11)', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    peticion(ctrl).flush(respuesta(pagina(FILAS)));
    await estabilizar();
    const enlace = filas(el)[0].querySelector('a.fs-codigo') as HTMLAnchorElement;
    expect(enlace.textContent?.trim()).toBe('RC-2026-000015');
    expect(enlace.getAttribute('href')).toBe('/reclamos/15');
    expect(filas(el)[0].textContent).toContain('Ver ›');
  });

  it('CA-3-28 marca "vencido" y "por vencer" con su indicador y el color de la fila', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    peticion(ctrl).flush(respuesta(pagina(FILAS)));
    await estabilizar();
    const [vencida, porVencer, respondida] = filas(el);
    expect(vencida.textContent).toContain('Vencido');
    expect(vencida.classList.contains('fs-fila--error')).toBe(true);
    expect(porVencer.textContent).toContain('Por vencer');
    expect(porVencer.classList.contains('fs-fila--aviso')).toBe(true);
    expect(respondida.textContent).not.toContain('Vencido');
    expect(respondida.textContent).not.toContain('Por vencer');
    expect(respondida.classList.contains('fs-fila--error')).toBe(false);
  });

  it('CA-3-28 filtra por estado desde la URL', async () => {
    const { ctrl } = await abrir('/reclamos?estado=REGISTRADO');
    const p = peticion(ctrl);
    expect(p.request.params.get('estado')).toBe('REGISTRADO');
    p.flush(respuesta(pagina([])));
  });

  it('CA-3-28 filtra por tipo, por plazo (por vencer / vencidos) y por texto', async () => {
    const { ctrl } = await abrir('/reclamos?tipo=QUEJA&plazo=porVencer&q=quispe');
    const p = peticion(ctrl);
    expect(p.request.params.get('tipo')).toBe('QUEJA');
    expect(p.request.params.get('porVencer')).toBe('true');
    expect(p.request.params.has('vencidos')).toBe(false);
    expect(p.request.params.get('q')).toBe('quispe');
    p.flush(respuesta(pagina([])));
  });

  it('CA-3-28 el filtro "vencidos" pide solo los vencidos', async () => {
    const { ctrl } = await abrir('/reclamos?plazo=vencidos');
    const p = peticion(ctrl);
    expect(p.request.params.get('vencidos')).toBe('true');
    expect(p.request.params.has('porVencer')).toBe(false);
    p.flush(respuesta(pagina([])));
  });

  it('CA-3-28 elegir un estado en los chips vuelve a consultar con ese estado', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    peticion(ctrl).flush(respuesta(pagina(FILAS)));
    await estabilizar();
    const chip = [...el.querySelectorAll('button, .fs-chip')].find((b) =>
      b.textContent?.trim().startsWith('Respondido'),
    ) as HTMLElement;
    chip.click();
    await estabilizar();
    const p = peticion(ctrl);
    expect(p.request.params.get('estado')).toBe('RESPONDIDO');
    p.flush(respuesta(pagina([FILAS[2]])));
  });

  it('CA-3-28 sin reclamos muestra el estado vacío', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    peticion(ctrl).flush(respuesta(pagina([])));
    await estabilizar();
    expect(el.textContent).toContain('Sin reclamos');
  });

  it('CA-3-28 si falla la carga muestra el error con "Reintentar"', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    peticion(ctrl).flush(
      { success: false, mensaje: 'x', codigo: 'ERROR_INTERNO' },
      { status: 500, statusText: 'x' },
    );
    await estabilizar();
    expect(el.querySelector('fs-estado-error')).not.toBeNull();
    (el.querySelector('fs-estado-error button') as HTMLButtonElement).click();
    await estabilizar();
    peticion(ctrl).flush(respuesta(pagina(FILAS)));
  });

  it('CA-3-31 el OPERADOR también ve la bandeja', async () => {
    const { el, estabilizar, ctrl } = await abrir('/reclamos', 'OPERADOR');
    peticion(ctrl).flush(respuesta(pagina(FILAS)));
    await estabilizar();
    expect(filas(el).length).toBe(3);
  });
});
