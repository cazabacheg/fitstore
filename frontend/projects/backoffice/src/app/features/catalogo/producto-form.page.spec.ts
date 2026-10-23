import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ToastService } from 'shared';
import { SESION_ADMIN, SESION_OPERADOR } from '../../testing/sesiones';
import { ProductoFormPage } from './producto-form.page';
import { MAESTROS_FORM, PRODUCTO, armar, asentar, atender, boton, sobre } from './prueba-catalogo';

@Component({ selector: 'fs-falso', template: 'lista' })
class Falso {}

const RUTAS = [
  { path: 'catalogo/productos', component: Falso },
  { path: 'catalogo/productos/nuevo', component: ProductoFormPage },
  { path: 'catalogo/productos/:id', component: ProductoFormPage },
];

async function abrir(producto = PRODUCTO, sesion = SESION_ADMIN) {
  const t = await armar(RUTAS, `/catalogo/productos/${producto.id}`, sesion);
  atender(t.ctrl, { ...MAESTROS_FORM, [`/admin/productos/${producto.id}`]: producto });
  await asentar(t.fixture);
  return t;
}
const pestana = async (t: Awaited<ReturnType<typeof abrir>>, nombre: string) => {
  boton(t.el, nombre)!.click();
  await asentar(t.fixture);
};
const poner = async (t: Awaited<ReturnType<typeof abrir>>, selector: string, valor: string) => {
  const campo = t.el.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
  await asentar(t.fixture);
};

describe('ProductoFormPage (P-2-04)', () => {
  it('CA-2-21 al crear un producto nuevo se guarda en BORRADOR y se abre su formulario', async () => {
    const t = await armar(RUTAS, '/catalogo/productos/nuevo');
    atender(t.ctrl, MAESTROS_FORM);
    await asentar(t.fixture);
    await poner(t, 'input[name="codigo"]', 'LEG02');
    await poner(t, 'input[name="nombre"]', 'Legging Aire');
    await poner(t, 'input[name="precioBase"]', '49.9');
    await poner(t, 'textarea[name="descripcion"]', 'Ligero');
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/productos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({
      codigo: 'LEG02',
      nombre: 'Legging Aire',
      precioBase: 49.9,
      descripcion: 'Ligero',
    });
    req.flush(sobre({ ...PRODUCTO, id: 9, codigo: 'LEG02', variantes: [] }));
    await asentar(t.fixture);
    expect(t.router.url).toBe('/catalogo/productos/9');
  });

  it('CA-2-22 genera la matriz de variantes con los ids elegidos', async () => {
    const t = await abrir({ ...PRODUCTO, variantes: [] });
    await pestana(t, 'Variantes');
    t.el.querySelectorAll<HTMLInputElement>('input[data-talla]').forEach((c) => c.click());
    t.el.querySelectorAll<HTMLInputElement>('input[data-color]').forEach((c) => c.click());
    await asentar(t.fixture);
    boton(t.el, 'Generar 15 variantes')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/productos/7/variantes/matriz');
    expect(req.request.body).toEqual({ tallaIds: [1, 2, 3, 4, 5], colorIds: [1, 2, 3] });
  });

  it('CA-2-22 la tabla de variantes muestra SKU, stock y el precio propio', async () => {
    const t = await abrir();
    await pestana(t, 'Variantes');
    expect(t.el.textContent).toContain('LEG01-NEG-S');
    expect(t.el.textContent).toContain('LEG01-AZU-S');
    expect(
      (t.el.querySelector('input[aria-label="Precio de LEG01-AZU-S"]') as HTMLInputElement).value,
    ).toBe('64.9');
  });

  it('CA-2-23 guardar el precio propio de una variante llama a PUT /admin/variantes/{id}', async () => {
    const t = await abrir();
    await pestana(t, 'Variantes');
    await poner(t, 'input[aria-label="Precio de LEG01-NEG-S"]', '55');
    boton(t.el.querySelector('tr[data-variante="101"]')!, 'Guardar')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/variantes/101');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ sku: 'LEG01-NEG-S', precio: 55 });
  });

  it('CA-2-24 si no se puede publicar muestra el aviso con lo que falta', async () => {
    const t = await abrir();
    boton(t.el, 'Publicar')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/productos/7/publicar');
    req.flush(
      {
        success: false,
        mensaje: 'Falta completar el producto para publicarlo',
        codigo: 'NO_PUBLICABLE',
        detalles: { faltantes: ['Imagen del color Azul'] },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await asentar(t.fixture);
    expect(t.el.querySelector('[role="alert"]')!.textContent).toContain('Imagen del color Azul');
  });

  it('CA-2-25 al publicar el producto completo queda ACTIVO', async () => {
    const t = await abrir();
    boton(t.el, 'Publicar')!.click();
    await asentar(t.fixture);
    t.ctrl
      .expectOne('/api/v1/admin/productos/7/publicar')
      .flush(sobre({ ...PRODUCTO, estado: 'ACTIVO' }));
    await asentar(t.fixture);
    expect(boton(t.el, 'Desactivar')).toBeDefined();
  });

  it('CA-2-28 el OPERADOR ve el formulario en solo lectura, sin guardar ni publicar', async () => {
    const t = await abrir(PRODUCTO, SESION_OPERADOR);
    expect(boton(t.el, 'Guardar')).toBeUndefined();
    expect(boton(t.el, 'Publicar')).toBeUndefined();
    expect(t.el.querySelector<HTMLInputElement>('input[name="nombre"]')!.disabled).toBe(true);
    await pestana(t, 'Variantes');
    expect(boton(t.el, 'Generar')).toBeUndefined();
  });

  it('CA-2-31 con 10 imágenes ya no se puede subir otra', async () => {
    const imagenes = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      colorId: 1,
      url: `/f/${i}.jpg`,
      urlMiniatura: `/f/${i}-400.jpg`,
      orden: i + 1,
      principal: i === 0,
    }));
    const t = await abrir({ ...PRODUCTO, imagenes });
    await pestana(t, 'Imágenes');
    expect(t.el.textContent).toContain('Ya tienes 10 imágenes');
    expect(t.el.querySelector('fs-subida-imagenes')).toBeNull();
  });

  it('CA-2-32 con varios colores la foto exige elegir su color antes de subir', async () => {
    const t = await abrir();
    await pestana(t, 'Imágenes');
    const subida = t.el.querySelector('fs-subida-imagenes')!;
    expect(subida).not.toBeNull();
    const archivo = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });
    const entrada = subida.querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(entrada, 'files', { value: [archivo] });
    entrada.dispatchEvent(new Event('change'));
    await asentar(t.fixture);
    const subir = boton(t.el, 'Subir 1 imagen') as HTMLButtonElement;
    expect(subir.disabled).toBe(true);
    expect(t.el.textContent).toContain('Elige el color de las fotos');
    const select = t.el.querySelector<HTMLSelectElement>('select[name="colorNuevo"]')!;
    select.value = '2';
    select.dispatchEvent(new Event('change'));
    await asentar(t.fixture);
    expect(subir.disabled).toBe(false);
    subir.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/productos/7/imagenes');
    expect((req.request.body as FormData).get('colorId')).toBe('2');
  });

  it('CA-2-73 si la miniatura guardada no carga, la pestaña Imágenes muestra un aviso en su lugar y no una imagen rota', async () => {
    const imagenes = [
      { id: 1, colorId: 1, url: '/f/0.jpg', urlMiniatura: '/f/0-400.jpg', orden: 1, principal: true, ancho: 1200, alto: 1500 },
    ];
    const t = await abrir({ ...PRODUCTO, imagenes });
    await pestana(t, 'Imágenes');
    t.el.querySelector('figure img')!.dispatchEvent(new Event('error'));
    await asentar(t.fixture);
    expect(t.el.querySelector('figure img')).toBeNull();
    expect(t.el.querySelector('figure')!.textContent).toContain('No se pudo cargar la miniatura');
  });

  it('CA-2-32 con un solo color, el color se asigna solo', async () => {
    const unico = { ...PRODUCTO, variantes: [PRODUCTO.variantes[0]] };
    const t = await abrir(unico);
    await pestana(t, 'Imágenes');
    expect(t.el.querySelector('select[name="colorNuevo"]')).toBeNull();
  });

  it('CA-2-32 marcar otra foto como principal se guarda', async () => {
    const imagenes = [
      {
        id: 1,
        colorId: 1,
        url: '/f/1.jpg',
        urlMiniatura: '/f/1-400.jpg',
        orden: 1,
        principal: true,
      },
      {
        id: 2,
        colorId: 1,
        url: '/f/2.jpg',
        urlMiniatura: '/f/2-400.jpg',
        orden: 2,
        principal: false,
      },
    ];
    const t = await abrir({ ...PRODUCTO, imagenes });
    await pestana(t, 'Imágenes');
    t.el.querySelector<HTMLInputElement>('input[type="radio"][data-imagen="2"]')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/imagenes/2');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ orden: 2, principal: true, colorId: 1 });
  });

  it('CA-2-38 el modo PERSONALIZADO muestra el editor de tramos y guarda el descuento del producto', async () => {
    const t = await abrir();
    await pestana(t, 'Descuento');
    expect(t.el.querySelector('fs-editor-tramos')).toBeNull();
    const modo = t.el.querySelector<HTMLSelectElement>('select[name="modoDescuento"]')!;
    modo.value = 'PERSONALIZADO';
    modo.dispatchEvent(new Event('change'));
    await asentar(t.fixture);
    expect(t.el.querySelector('fs-editor-tramos')).not.toBeNull();
    boton(t.el, 'Agregar tramo')!.click();
    await asentar(t.fixture);
    await poner(t, 'input[aria-label="Unidades del tramo 1"]', '3');
    await poner(t, 'input[aria-label="Porcentaje del tramo 1"]', '5');
    boton(t.el, 'Guardar descuento')!.click();
    await asentar(t.fixture);
    const req = t.ctrl.expectOne('/api/v1/admin/productos/7/descuento');
    expect(req.request.body).toEqual({
      modo: 'PERSONALIZADO',
      tramos: [{ unidadesMinimas: 3, porcentaje: 5 }],
    });
  });
});

describe('ProductoFormPage: categorías, validación y errores del servidor', () => {
  const nuevo = async () => {
    const t = await armar(RUTAS, '/catalogo/productos/nuevo');
    atender(t.ctrl, MAESTROS_FORM);
    await asentar(t.fixture);
    return t;
  };
  const marca = (t: Awaited<ReturnType<typeof abrir>>, id: number) =>
    t.el.querySelector<HTMLInputElement>(`input[data-categoria="${id}"]`)!;
  const alternar = async (t: Awaited<ReturnType<typeof abrir>>, id: number) => {
    marca(t, id).click();
    await asentar(t.fixture);
  };
  const llenarValido = async (t: Awaited<ReturnType<typeof abrir>>) => {
    await poner(t, 'input[name="codigo"]', 'LEG02');
    await poner(t, 'input[name="nombre"]', 'Legging Aire');
    await poner(t, 'input[name="precioBase"]', '49.9');
  };
  const error = (t: Awaited<ReturnType<typeof abrir>>, campo: string) =>
    t.el.querySelector(`[data-error="${campo}"]`)?.textContent?.trim();

  it('CA-2-70 al marcar una subcategoría se marca también su categoría padre', async () => {
    const t = await nuevo();
    await alternar(t, 11);
    expect(marca(t, 11).checked).toBe(true);
    expect(marca(t, 10).checked).toBe(true);
    await llenarValido(t);
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    expect(t.ctrl.expectOne('/api/v1/admin/productos').request.body.categoriaIds).toEqual([11, 10]);
  });

  it('CA-2-70 al desmarcar la categoría padre se desmarcan también sus subcategorías', async () => {
    const t = await nuevo();
    await alternar(t, 11);
    await alternar(t, 10);
    expect(marca(t, 10).checked).toBe(false);
    expect(marca(t, 11).checked).toBe(false);
  });

  it('CA-2-70 marcar solo el padre no marca sus subcategorías, y desmarcar una subcategoría deja al padre marcado', async () => {
    const t = await nuevo();
    await alternar(t, 10);
    expect(marca(t, 11).checked).toBe(false);
    await alternar(t, 11);
    await alternar(t, 11);
    expect(marca(t, 11).checked).toBe(false);
    expect(marca(t, 10).checked).toBe(true);
  });

  it('CA-2-71 un código con minúsculas o símbolos no se envía y el error aparece bajo el campo', async () => {
    const t = await nuevo();
    await llenarValido(t);
    await poner(t, 'input[name="codigo"]', 'leg-1');
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    t.ctrl.expectNone('/api/v1/admin/productos');
    expect(error(t, 'codigo')).toBe('El código usa de 2 a 20 letras mayúsculas o números');
    expect(t.el.querySelector('input[name="codigo"]')!.getAttribute('aria-invalid')).toBe('true');
  });

  it('CA-2-71 nombre corto y precio vacío también se validan antes de enviar, cada uno bajo su campo', async () => {
    const t = await nuevo();
    await poner(t, 'input[name="codigo"]', 'LEG02');
    await poner(t, 'input[name="nombre"]', 'ab');
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    t.ctrl.expectNone('/api/v1/admin/productos');
    expect(error(t, 'nombre')).toBe('El nombre debe tener de 3 a 120 caracteres');
    expect(error(t, 'precioBase')).toBe('Ingresa un precio igual o mayor que 0, con hasta 2 decimales');
    expect(error(t, 'codigo')).toBeUndefined();
  });

  it('CA-2-72 si el servidor rechaza el código, el error de ese campo aparece bajo él y no se pierde lo escrito', async () => {
    const t = await nuevo();
    await llenarValido(t);
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    t.ctrl.expectOne('/api/v1/admin/productos').flush(
      {
        success: false,
        mensaje: 'Revisa los datos ingresados',
        codigo: 'VALIDACION',
        errores: [{ campo: 'codigo', mensaje: 'El código usa de 2 a 20 letras mayúsculas o números' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await asentar(t.fixture);
    expect(error(t, 'codigo')).toBe('El código usa de 2 a 20 letras mayúsculas o números');
    expect(t.el.querySelector<HTMLInputElement>('input[name="nombre"]')!.value).toBe('Legging Aire');
    // al corregir el campo, su error desaparece
    await poner(t, 'input[name="codigo"]', 'LEG03');
    expect(error(t, 'codigo')).toBeUndefined();
  });

  it('CA-2-72 un error de campo que el formulario no tiene (p. ej. categoriaIds) se muestra en un aviso visible', async () => {
    const t = await nuevo();
    await llenarValido(t);
    boton(t.el, 'Guardar')!.click();
    await asentar(t.fixture);
    t.ctrl.expectOne('/api/v1/admin/productos').flush(
      {
        success: false,
        mensaje: 'Revisa los datos ingresados',
        codigo: 'VALIDACION',
        errores: [{ campo: 'categoriaIds', mensaje: 'Hay categorías que no existen' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await asentar(t.fixture);
    expect(TestBed.inject(ToastService).toasts().map((a) => a.mensaje)).toEqual([
      'Hay categorías que no existen',
    ]);
  });
});
