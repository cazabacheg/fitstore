import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { abrirRuta, reclamo, respuesta } from './datos-prueba';
import type { settingsApi } from 'shared';

const URL = '/api/v1/admin/reclamos/15';
const CONFIRMA = { provide: DialogoService, useValue: { confirmar: () => of(true) } };

async function abrir(
  rol: 'ADMIN' | 'OPERADOR' = 'ADMIN',
  datos: settingsApi.Reclamo = reclamo(15),
) {
  const montaje = await abrirRuta('/reclamos/15', rol, [CONFIRMA]);
  const ctrl = TestBed.inject(HttpTestingController);
  ctrl.expectOne(URL).flush(respuesta(datos));
  await montaje.estabilizar();
  return { ...montaje, ctrl };
}

const q = (el: HTMLElement, id: string) => el.querySelector(`[data-testid="${id}"]`);

describe('ReclamoDetallePage (P-3-06)', () => {
  it('CA-3-28 muestra la hoja completa: número, estado, plazo, consumidor, bien, detalle y pedido', async () => {
    const { el } = await abrir();
    expect(el.textContent).toContain('RC-2026-000015');
    expect(q(el, 'estado')!.textContent).toContain('Registrado');
    expect(el.textContent).toContain('09/11/2026');
    expect(el.textContent).toContain('Rosa Díaz');
    expect(el.textContent).toContain('87654321');
    expect(el.textContent).toContain('rosa.diaz@correo.com');
    expect(el.textContent).toContain('Calle Mercaderes 456');
    expect(el.textContent).toContain('Casaca Wind talla M negra');
    expect(el.textContent).toContain('S/ 89.90');
    expect(el.textContent).toContain('FS-000118');
    expect(el.textContent).toContain('La casaca llegó con el cierre roto.');
    expect(el.textContent).toContain('Cambio por una nueva');
  });

  it('CA-3-28 indica "vencido" o "por vencer" en el detalle', async () => {
    const { el } = await abrir('ADMIN', reclamo(15, { vencido: true }));
    expect(q(el, 'plazo')!.textContent).toContain('Vencido');
  });

  it('CA-3-24 si es menor de edad muestra el nombre del apoderado', async () => {
    const base = reclamo(15);
    const { el } = await abrir('ADMIN', {
      ...base,
      consumidor: { ...base.consumidor, esMenor: true, apoderadoNombre: 'Marta Díaz Pérez' },
    });
    expect(el.textContent).toContain('Menor de edad');
    expect(el.textContent).toContain('Marta Díaz Pérez');
  });

  it('CA-3-29 el ADMIN toma un reclamo registrado y pasa a EN_ATENCION', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    (q(el, 'tomar') as HTMLButtonElement).click();
    await estabilizar();
    const patch = ctrl.expectOne(URL + '/en-atencion');
    expect(patch.request.method).toBe('PATCH');
    patch.flush(respuesta(reclamo(15, { estado: 'EN_ATENCION' })));
    await estabilizar();
    expect(q(el, 'estado')!.textContent).toContain('En atención');
    expect(q(el, 'tomar')).toBeNull();
    expect(q(el, 'respuesta')).not.toBeNull();
  });

  it('CA-3-30 el ADMIN responde: pide confirmación, envía la respuesta y el reclamo queda RESPONDIDO e inmutable', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const texto = q(el, 'respuesta') as HTMLTextAreaElement;
    texto.value = 'Procedemos con el cambio por una casaca nueva sin costo.';
    texto.dispatchEvent(new Event('input', { bubbles: true }));
    await estabilizar();
    (q(el, 'responder') as HTMLButtonElement).click();
    await estabilizar();

    const post = ctrl.expectOne(URL + '/respuesta');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({
      respuesta: 'Procedemos con el cambio por una casaca nueva sin costo.',
    });
    post.flush(
      respuesta(
        reclamo(15, {
          estado: 'RESPONDIDO',
          respuesta: 'Procedemos con el cambio por una casaca nueva sin costo.',
          respondidoEn: '2026-10-19T16:00:00Z',
          respondidoPor: 'admin',
        }),
      ),
    );
    await estabilizar();
    expect(q(el, 'estado')!.textContent).toContain('Respondido');
    expect(q(el, 'respuesta-registrada')!.textContent).toContain('Procedemos con el cambio');
    expect(q(el, 'responder')).toBeNull();
    expect(q(el, 'tomar')).toBeNull();
    expect(q(el, 'respuesta')).toBeNull();
  });

  it('CA-3-30 la respuesta debe tener de 10 a 3000 caracteres', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const texto = q(el, 'respuesta') as HTMLTextAreaElement;
    texto.value = 'corta';
    texto.dispatchEvent(new Event('input', { bubbles: true }));
    await estabilizar();
    expect((q(el, 'responder') as HTMLButtonElement).disabled).toBe(true);
    ctrl.expectNone(URL + '/respuesta');
    texto.value = 'x'.repeat(3001);
    texto.dispatchEvent(new Event('input', { bubbles: true }));
    await estabilizar();
    expect((q(el, 'responder') as HTMLButtonElement).disabled).toBe(true);
  });

  it('CA-3-30 un reclamo respondido muestra la respuesta, la fecha y quién respondió, sin acciones', async () => {
    const { el } = await abrir(
      'ADMIN',
      reclamo(15, {
        estado: 'RESPONDIDO',
        respuesta: 'Ya lo resolvimos.',
        respondidoEn: '2026-10-20T15:00:00Z',
        respondidoPor: 'admin',
      }),
    );
    expect(q(el, 'respuesta-registrada')!.textContent).toContain('Ya lo resolvimos.');
    expect(q(el, 'respuesta-registrada')!.textContent).toContain('20/10/2026');
    expect(q(el, 'tomar')).toBeNull();
    expect(q(el, 'responder')).toBeNull();
  });

  it('CA-3-30 si el servidor responde 409 (ya respondido) avisa y recarga el reclamo', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    const texto = q(el, 'respuesta') as HTMLTextAreaElement;
    texto.value = 'Procedemos con el cambio del producto.';
    texto.dispatchEvent(new Event('input', { bubbles: true }));
    await estabilizar();
    (q(el, 'responder') as HTMLButtonElement).click();
    await estabilizar();
    ctrl.expectOne(URL + '/respuesta').flush(
      {
        success: false,
        mensaje: 'El reclamo ya fue respondido y no se puede modificar',
        codigo: 'ESTADO_INVALIDO',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await estabilizar();
    expect(q(el, 'error-accion')!.textContent).toContain('ya fue respondido');
    ctrl
      .expectOne(URL)
      .flush(
        respuesta(reclamo(15, { estado: 'RESPONDIDO', respuesta: 'Otra persona respondió.' })),
      );
    await estabilizar();
    expect(q(el, 'respuesta-registrada')!.textContent).toContain('Otra persona respondió.');
  });

  it('CA-3-31 el OPERADOR ve el detalle pero no tiene "Tomar" ni "Responder"', async () => {
    const { el } = await abrir('OPERADOR');
    expect(el.textContent).toContain('La casaca llegó con el cierre roto.');
    expect(q(el, 'tomar')).toBeNull();
    expect(q(el, 'responder')).toBeNull();
    expect(q(el, 'respuesta')).toBeNull();
    expect(el.textContent).toContain('Solo un administrador');
  });

  it('CA-3-28 si el reclamo no existe muestra "no encontrado"', async () => {
    const montaje = await abrirRuta('/reclamos/15', 'ADMIN', [CONFIRMA]);
    TestBed.inject(HttpTestingController)
      .expectOne(URL)
      .flush(
        { success: false, mensaje: 'No encontramos lo que buscas', codigo: 'NO_ENCONTRADO' },
        { status: 404, statusText: 'x' },
      );
    await montaje.estabilizar();
    expect(q(montaje.el, 'no-encontrado')).not.toBeNull();
  });
});
