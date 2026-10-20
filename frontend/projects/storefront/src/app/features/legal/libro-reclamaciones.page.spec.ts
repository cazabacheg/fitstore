import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { provideFitstoreHttp } from 'shared';
import { montar } from '../../testing';
import { configuracionPublica, respuesta } from '../inicio/datos-prueba';
import { LibroReclamacionesPage } from './libro-reclamaciones.page';

type Campo = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

/** Busca el campo cuya etiqueta empieza con el texto dado (input, select, textarea o casilla). */
function campo(el: HTMLElement, etiqueta: string): Campo {
  const etiquetas = [...el.querySelectorAll('label')];
  const label = etiquetas.find((l) => l.textContent?.replace(/\s+/g, ' ').trim().startsWith(etiqueta));
  if (!label) throw new Error(`No hay un campo "${etiqueta}"`);
  const destino = label.htmlFor ? el.querySelector(`#${label.htmlFor}`) : label.querySelector('input,select,textarea');
  return destino as Campo;
}

function existe(el: HTMLElement, etiqueta: string): boolean {
  return [...el.querySelectorAll('label')].some((l) =>
    l.textContent?.replace(/\s+/g, ' ').trim().startsWith(etiqueta),
  );
}

async function escribir(el: HTMLElement, etiqueta: string, valor: string, estabilizar: () => Promise<void>) {
  const c = campo(el, etiqueta);
  c.value = valor;
  c.dispatchEvent(new Event(c instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }));
  await estabilizar();
}

async function marcar(el: HTMLElement, etiqueta: string, estabilizar: () => Promise<void>) {
  const c = campo(el, etiqueta) as HTMLInputElement;
  c.click();
  await estabilizar();
}

async function llenarValido(el: HTMLElement, estabilizar: () => Promise<void>) {
  await escribir(el, 'Nombres', 'Rosa', estabilizar);
  await escribir(el, 'Apellidos', 'Díaz', estabilizar);
  await escribir(el, 'Tipo de documento', 'DNI', estabilizar);
  await escribir(el, 'Número de documento', '87654321', estabilizar);
  await escribir(el, 'Domicilio', 'Calle Mercaderes 456, Cercado, Arequipa', estabilizar);
  await escribir(el, 'Teléfono', '912345678', estabilizar);
  await escribir(el, 'Correo electrónico', 'rosa.diaz@correo.com', estabilizar);
  await escribir(el, 'Bien contratado', 'PRODUCTO', estabilizar);
  await escribir(el, 'Descripción del bien', 'Casaca Wind talla M negra', estabilizar);
  await escribir(el, 'Monto reclamado', '89.9', estabilizar);
  await escribir(el, 'Código de pedido', 'FS-000118', estabilizar);
  await escribir(el, 'Detalle', 'La casaca llegó con el cierre roto.', estabilizar);
  await escribir(el, 'Pedido del consumidor', 'Cambio por una nueva o devolución del dinero.', estabilizar);
}

async function abrir() {
  const montaje = await montar(LibroReclamacionesPage, {}, [
    provideRouter([]),
    provideFitstoreHttp({ carrito: false }),
    provideHttpClientTesting(),
  ]);
  const ctrl = TestBed.inject(HttpTestingController);
  ctrl.expectOne('/api/v1/settings/public').flush(respuesta(configuracionPublica()));
  await montaje.estabilizar();
  return { ...montaje, ctrl };
}

const enviar = (el: HTMLElement) => el.querySelector('button[type="submit"]') as HTMLButtonElement;

describe('LibroReclamacionesPage (P-3-03)', () => {
  it('CA-3-23 muestra los datos del proveedor: razón social, RUC y dirección fiscal', async () => {
    const { el } = await abrir();
    const proveedor = el.querySelector('[data-testid="proveedor"]')!;
    expect(proveedor.textContent).toContain('Moda Activa S.A.C.');
    expect(proveedor.textContent).toContain('20123456789');
    expect(proveedor.textContent).toContain('Jr. Gamarra 700, La Victoria, Lima');
  });

  it('CA-3-23 trae el formulario de la Hoja de Reclamación y explica qué es un reclamo y una queja', async () => {
    const { el } = await abrir();
    for (const etiqueta of [
      'Nombres',
      'Apellidos',
      'Tipo de documento',
      'Número de documento',
      'Domicilio',
      'Teléfono',
      'Correo electrónico',
      'Soy menor de edad',
      'Bien contratado',
      'Descripción del bien',
      'Monto reclamado',
      'Código de pedido',
      'Detalle',
      'Pedido del consumidor',
    ]) {
      expect(existe(el, etiqueta), etiqueta).toBe(true);
    }
    expect(el.querySelector('input[type="radio"][value="RECLAMO"]')).not.toBeNull();
    expect(el.querySelector('input[type="radio"][value="QUEJA"]')).not.toBeNull();
    expect(el.textContent).toContain('disconformidad con el producto o servicio');
    expect(el.textContent).toContain('malestar con la atención');
  });

  it('CA-3-23 enviar vacío no llama a la API y señala los campos obligatorios', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    enviar(el).click();
    await estabilizar();
    ctrl.expectNone('/api/v1/reclamos');
    expect(campo(el, 'Nombres').getAttribute('aria-invalid')).toBe('true');
    expect(campo(el, 'Correo electrónico').getAttribute('aria-invalid')).toBe('true');
    expect(el.querySelectorAll('.fs-campo__error').length).toBeGreaterThanOrEqual(8);
  });

  it('CA-3-23 el DNI debe tener 8 dígitos; el carné de extranjería admite de 8 a 12 caracteres', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    await escribir(el, 'Número de documento', '1234567', estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl.expectNone('/api/v1/reclamos');
    expect(campo(el, 'Número de documento').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('El DNI debe tener 8 dígitos');

    await escribir(el, 'Tipo de documento', 'CE', estabilizar);
    await escribir(el, 'Número de documento', 'CE0012345X', estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl.expectOne('/api/v1/reclamos');
  });

  it('CA-3-23 valida el correo, el monto (no negativo) y el código de pedido (FS-000118)', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    await escribir(el, 'Correo electrónico', 'no-es-correo', estabilizar);
    await escribir(el, 'Monto reclamado', '-5', estabilizar);
    await escribir(el, 'Código de pedido', 'FS-12', estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl.expectNone('/api/v1/reclamos');
    expect(campo(el, 'Correo electrónico').getAttribute('aria-invalid')).toBe('true');
    expect(campo(el, 'Monto reclamado').getAttribute('aria-invalid')).toBe('true');
    expect(campo(el, 'Código de pedido').getAttribute('aria-invalid')).toBe('true');
  });

  it('CA-3-24 marcar "soy menor de edad" exige el nombre del padre, madre o apoderado', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    expect(existe(el, 'Nombre del padre, madre o apoderado')).toBe(false);
    await llenarValido(el, estabilizar);

    await marcar(el, 'Soy menor de edad', estabilizar);
    expect(existe(el, 'Nombre del padre, madre o apoderado')).toBe(true);
    enviar(el).click();
    await estabilizar();
    ctrl.expectNone('/api/v1/reclamos');
    expect(campo(el, 'Nombre del padre, madre o apoderado').getAttribute('aria-invalid')).toBe('true');

    await escribir(el, 'Nombre del padre, madre o apoderado', 'Marta Díaz Pérez', estabilizar);
    enviar(el).click();
    await estabilizar();
    const peticion = ctrl.expectOne('/api/v1/reclamos');
    expect(peticion.request.body.consumidor.esMenor).toBe(true);
    expect(peticion.request.body.consumidor.apoderadoNombre).toBe('Marta Díaz Pérez');
  });

  it('CA-3-24 al desmarcar "menor de edad" el apoderado deja de enviarse', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    await marcar(el, 'Soy menor de edad', estabilizar);
    await escribir(el, 'Nombre del padre, madre o apoderado', 'Marta Díaz Pérez', estabilizar);
    await marcar(el, 'Soy menor de edad', estabilizar);
    expect(existe(el, 'Nombre del padre, madre o apoderado')).toBe(false);
    enviar(el).click();
    await estabilizar();
    const cuerpo = ctrl.expectOne('/api/v1/reclamos').request.body;
    expect(cuerpo.consumidor.esMenor).toBe(false);
    expect(cuerpo.consumidor.apoderadoNombre).toBeNull();
  });

  it('CA-3-25 envía la hoja y muestra el número y la fecha límite en la confirmación', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    (el.querySelector('input[type="radio"][value="QUEJA"]') as HTMLInputElement).click();
    await estabilizar();
    enviar(el).click();
    await estabilizar();

    const peticion = ctrl.expectOne('/api/v1/reclamos');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      tipo: 'QUEJA',
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
      bien: { tipo: 'PRODUCTO', descripcion: 'Casaca Wind talla M negra', montoReclamado: 89.9, pedidoCodigo: 'FS-000118' },
      detalle: 'La casaca llegó con el cierre roto.',
      pedidoConsumidor: 'Cambio por una nueva o devolución del dinero.',
    });
    peticion.flush(
      respuesta({ numero: 'RC-2026-000015', fechaLimite: '2026-11-09', creadoEn: '2026-10-19T15:20:00Z' }),
      { status: 201, statusText: 'Created' },
    );
    await estabilizar();

    const confirmacion = el.querySelector('[data-testid="confirmacion"]')!;
    expect(confirmacion.textContent).toContain('RC-2026-000015');
    expect(confirmacion.textContent).toContain('9 de noviembre de 2026');
    expect(confirmacion.textContent).toContain('15 días hábiles');
    expect(confirmacion.textContent).toContain('rosa.diaz@correo.com');
    expect(el.querySelector('form')).toBeNull();
  });

  it('CA-3-25 la confirmación se puede imprimir', async () => {
    const imprimir = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl
      .expectOne('/api/v1/reclamos')
      .flush(respuesta({ numero: 'RC-2026-000015', fechaLimite: '2026-11-09', creadoEn: '2026-10-19T15:20:00Z' }));
    await estabilizar();

    (el.querySelector('[data-testid="imprimir"]') as HTMLButtonElement).click();
    expect(imprimir).toHaveBeenCalledOnce();
    imprimir.mockRestore();
  });

  it('CA-3-25 sin monto, sin código de pedido y sin apoderado la hoja los envía vacíos (null)', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    await escribir(el, 'Monto reclamado', '', estabilizar);
    await escribir(el, 'Código de pedido', '', estabilizar);
    enviar(el).click();
    await estabilizar();
    const bien = ctrl.expectOne('/api/v1/reclamos').request.body.bien;
    expect(bien.montoReclamado).toBeNull();
    expect(bien.pedidoCodigo).toBeNull();
  });

  it('CA-3-25 mientras se envía el botón queda deshabilitado y no se duplica el reclamo', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    enviar(el).click();
    enviar(el).click();
    await estabilizar();
    expect(enviar(el).disabled).toBe(true);
    ctrl.expectOne('/api/v1/reclamos');
  });

  it('CA-3-23 los errores del servidor se muestran en su campo', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl.expectOne('/api/v1/reclamos').flush(
      {
        success: false,
        mensaje: 'Revisa los datos ingresados',
        codigo: 'VALIDACION',
        errores: [{ campo: 'consumidor.numeroDocumento', mensaje: 'El documento ya no es válido' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await estabilizar();
    expect(campo(el, 'Número de documento').getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('El documento ya no es válido');
    expect(el.querySelector('form')).not.toBeNull();
  });

  it('CA-3-27 si el gateway responde 429 avisa que espere y conserva lo escrito', async () => {
    const { el, estabilizar, ctrl } = await abrir();
    await llenarValido(el, estabilizar);
    enviar(el).click();
    await estabilizar();
    ctrl.expectOne('/api/v1/reclamos').flush(
      { success: false, mensaje: 'Demasiados intentos, espera un momento', codigo: 'DEMASIADOS_INTENTOS' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    await estabilizar();
    expect(el.querySelector('[data-testid="error-envio"]')!.textContent).toContain('Demasiados intentos');
    expect((campo(el, 'Nombres') as HTMLInputElement).value).toBe('Rosa');
    expect(enviar(el).disabled).toBe(false);
  });
});
