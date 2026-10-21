import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import {
  configuracionAdmin,
  errorApi,
  escribir,
  montarComponente,
  porId,
  respuesta,
} from './datos-prueba';
import { ImagenesLanding } from './imagenes-landing';

const IMAGENES = configuracionAdmin().contenido.imagenesLanding;
const BASE = '/api/v1/admin/settings/imagenes';

const imagen = (id: number, orden: number) => ({
  id,
  tipo: 'LANDING' as const,
  url: `/media/tienda/landing/${id}.jpg`,
  orden,
  textoAlternativo: `Foto ${id}`,
  enlace: null,
});

function montar(imagenes = IMAGENES, confirmar = true) {
  return montarComponente(ImagenesLanding, { imagenes }, [
    { provide: DialogoService, useValue: { confirmar: () => of(confirmar) } },
  ]);
}

const filas = (el: HTMLElement) => [...el.querySelectorAll('[data-testid="imagen-fila"]')];

describe('ImagenesLanding (P-3-04, Contenido)', () => {
  it('CA-3-14 lista las imágenes de la landing en su orden con su texto alternativo', async () => {
    const { el } = await montar();
    expect(filas(el).length).toBe(2);
    expect((el.querySelectorAll('[data-testid="imagen-alt"]')[0] as HTMLInputElement).value).toBe(
      'Foto uno',
    );
    expect(el.querySelectorAll('img')[0].getAttribute('src')).toBe('/media/tienda/landing/1.jpg');
  });

  it('CA-3-14 sube una imagen (JPG o WebP) como LANDING con su texto alternativo y enlace', async () => {
    const { el, estabilizar, comp } = await montar();
    const cambios: unknown[] = [];
    (comp as ImagenesLanding).cambio.subscribe((c) => cambios.push(c));
    const archivo = new File([new Uint8Array(1000)], 'campana.jpg', { type: 'image/jpeg' });
    const subida = (comp as ImagenesLanding).subir([archivo]);
    const ctrl = TestBed.inject(HttpTestingController);
    const peticion = ctrl.expectOne(BASE);
    expect(peticion.request.method).toBe('POST');
    const cuerpo = peticion.request.body as FormData;
    expect(cuerpo.get('tipo')).toBe('LANDING');
    expect((cuerpo.get('archivo') as File).name).toBe('campana.jpg');
    peticion.flush(respuesta({ ...imagen(13, 2), textoAlternativo: null }), {
      status: 201,
      statusText: 'Created',
    });
    await subida;
    await estabilizar();
    expect(filas(el).length).toBe(3);
    expect(cambios.length).toBe(1);
  });

  it('CA-3-14 la sexta imagen se rechaza: con 5 ya no se pueden subir más', async () => {
    const cinco = [1, 2, 3, 4, 5].map((i) => imagen(i, i - 1));
    const { el } = await montar(cinco);
    expect(el.textContent).toContain('Ya tienes 5 imágenes');
    expect(el.querySelector('fs-subida-imagenes')).toBeNull();
  });

  it('CA-3-14 si el servidor responde 409 LIMITE_IMAGENES lo muestra', async () => {
    const { el, estabilizar, comp } = await montar();
    const archivo = new File([new Uint8Array(1000)], 'otra.jpg', { type: 'image/jpeg' });
    const subida = (comp as ImagenesLanding).subir([archivo]);
    const ctrl = TestBed.inject(HttpTestingController);
    ctrl.expectOne(BASE).flush(errorApi('LIMITE_IMAGENES', 'La landing admite hasta 5 imágenes'), {
      status: 409,
      statusText: 'Conflict',
    });
    await subida;
    await estabilizar();
    expect(porId(el, 'error-imagenes').textContent).toContain('hasta 5 imágenes');
  });

  it('CA-3-14 mover una imagen hacia abajo guarda el nuevo orden de las dos afectadas', async () => {
    const { el, estabilizar, comp } = await montar();
    const cambios: { id: number; orden: number }[][] = [];
    (comp as ImagenesLanding).cambio.subscribe((c) =>
      cambios.push(c.map((i) => ({ id: i.id, orden: i.orden }))),
    );
    (el.querySelectorAll('[data-testid="bajar"]')[0] as HTMLButtonElement).click();
    await estabilizar();

    const ctrl = TestBed.inject(HttpTestingController);
    const puts = ctrl.match((r) => r.method === 'PUT' && r.url.startsWith(BASE + '/'));
    expect(puts.map((p) => [p.request.url.split('/').pop(), p.request.body.orden]).sort()).toEqual([
      ['11', 1],
      ['12', 0],
    ]);
    puts.forEach((p) =>
      p.flush(respuesta(imagen(Number(p.request.url.split('/').pop()), p.request.body.orden))),
    );
    await estabilizar();
    expect((el.querySelectorAll('[data-testid="imagen-alt"]')[0] as HTMLInputElement).value).toBe(
      'Foto dos',
    );
  });

  it('CA-3-14 la primera no se puede subir y la última no se puede bajar', async () => {
    const { el } = await montar();
    expect((el.querySelectorAll('[data-testid="subir"]')[0] as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect((el.querySelectorAll('[data-testid="bajar"]')[1] as HTMLButtonElement).disabled).toBe(
      true,
    );
  });

  it('CA-3-14 cambia el texto alternativo y el enlace de una imagen', async () => {
    const { el, estabilizar } = await montar();
    escribir(el.querySelectorAll('[data-testid="imagen-alt"]')[0], 'Modelo con leggings');
    escribir(
      el.querySelectorAll('[data-testid="imagen-enlace"]')[0],
      '/productos?categoria=leggings',
    );
    await estabilizar();
    (el.querySelectorAll('[data-testid="guardar-imagen"]')[0] as HTMLButtonElement).click();
    await estabilizar();
    const peticion = TestBed.inject(HttpTestingController).expectOne(BASE + '/11');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      orden: 0,
      textoAlternativo: 'Modelo con leggings',
      enlace: '/productos?categoria=leggings',
    });
    peticion.flush(respuesta({ ...imagen(11, 0), enlace: '/productos?categoria=leggings' }));
  });

  it('CA-3-14 un enlace de imagen inválido no se envía', async () => {
    const { el, estabilizar } = await montar();
    escribir(el.querySelectorAll('[data-testid="imagen-enlace"]')[0], 'javascript:alert(1)');
    await estabilizar();
    (el.querySelectorAll('[data-testid="guardar-imagen"]')[0] as HTMLButtonElement).click();
    await estabilizar();
    TestBed.inject(HttpTestingController).expectNone(BASE + '/11');
    expect(
      el.querySelectorAll('[data-testid="imagen-enlace"]')[0].getAttribute('aria-invalid'),
    ).toBe('true');
  });

  it('CA-3-14 eliminar pide confirmación y borra la imagen', async () => {
    const { el, estabilizar } = await montar();
    (el.querySelectorAll('[data-testid="eliminar-imagen"]')[0] as HTMLButtonElement).click();
    await estabilizar();
    const peticion = TestBed.inject(HttpTestingController).expectOne(BASE + '/11');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(respuesta(null));
    await estabilizar();
    expect(filas(el).length).toBe(1);
  });

  it('CA-3-14 si no se confirma, no se elimina', async () => {
    const { el, estabilizar } = await montar(IMAGENES, false);
    (el.querySelectorAll('[data-testid="eliminar-imagen"]')[0] as HTMLButtonElement).click();
    await estabilizar();
    TestBed.inject(HttpTestingController).expectNone(BASE + '/11');
    expect(filas(el).length).toBe(2);
  });
});
