import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { RouterTestingHarness } from '@angular/router/testing';
import { orderApi } from 'shared';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { DetallePedidoAdminPage } from './detalle-pedido-admin.page';
import {
  DialogoAccionPedido,
  DialogoAccionPedidoService,
  type DatosAccionPedido,
  type ResultadoAccionPedido,
} from './dialogo-accion-pedido';
import { admin, errorApi, http, ok, proveedores } from './pruebas-pedidos';

const ID = '9d3c1f2e-0000-4000-8000-000000000001';
const RUTA = `/api/v1/admin/pedidos/${ID}`;
const abrirDialogo = vi.fn<(datos: DatosAccionPedido) => unknown>();

async function abrir(datos: orderApi.PedidoAdmin) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      ...proveedores([{ path: 'pedidos/:id', component: DetallePedidoAdminPage }]),
      { provide: DialogoAccionPedidoService, useValue: { abrir: abrirDialogo } },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(`/pedidos/${ID}`, DetallePedidoAdminPage);
  harness.detectChanges();
  http().expectOne(RUTA).flush(ok(datos));
  await actualizar(harness);
  const el = harness.routeNativeElement as HTMLElement;
  const boton = (texto: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(texto));
  return { harness, el, boton };
}

async function actualizar(harness: RouterTestingHarness) {
  harness.detectChanges();
  await harness.fixture.whenStable();
}

describe('Detalle de pedido del backoffice (P-4-06)', () => {
  afterEach(() => {
    abrirDialogo.mockReset();
    http().verify();
  });

  it('CA-4-43 muestra cliente, destinatario, entrega, productos con SKU enlazado, montos, pago, boleta y el historial', async () => {
    const p = await abrir(
      admin({
        historial: [
          { fecha: '2026-10-20T15:00:00Z', tipo: 'CAMBIO_ESTADO', estadoNuevo: 'PENDIENTE_PAGO', actorTipo: 'CLIENTE' },
          { fecha: '2026-10-20T16:00:00Z', tipo: 'CAMBIO_ESTADO', estadoNuevo: 'EN_PREPARACION', actorTipo: 'STAFF', actorNombre: 'Diana Flores' },
        ],
      }),
    );

    const texto = p.el.textContent ?? '';
    expect(texto).toContain('Pedido FS-000124');
    expect(texto).toContain('Ana Quispe');
    expect(texto).toContain('DNI 12345678');
    expect(texto).toContain('Av. Larco 345, Miraflores');
    expect(texto).toContain('Legging Pro');
    expect(texto).toContain('S/ 129.80');
    expect(texto).toContain('Pagado con SIMULADO · TARJETA');
    expect(texto).toContain('Descargar boleta');
    expect(texto).toContain('Diana Flores');
    const sku = [...p.el.querySelectorAll<HTMLAnchorElement>('a')].find((a) => a.textContent?.includes('LEG01-NEG-M'))!;
    expect(sku.getAttribute('href')).toBe('/inventario/10002');
    const cliente = [...p.el.querySelectorAll<HTMLAnchorElement>('a')].find((a) => a.textContent?.includes('Ver sus pedidos'))!;
    expect(cliente.getAttribute('href')).toContain('clienteId=35a11d4f');
  });

  it('CA-4-43 solo dibuja los botones de las transiciones permitidas; un pedido final no tiene acciones', async () => {
    const p = await abrir(admin());
    expect(p.boton('Empezar a preparar')).toBeDefined();
    expect(p.boton('Marcar listo para despacho')).toBeUndefined();
    expect(p.boton('Cancelar pedido')).toBeDefined();

    TestBed.resetTestingModule();
    const final = await abrir(admin({ estado: 'ENTREGADO', transicionesPermitidas: [], puedeCancelarStaff: false }));
    expect(final.boton('Empezar a preparar')).toBeUndefined();
    expect(final.boton('Cancelar pedido')).toBeUndefined();
    expect(final.el.textContent).toContain('Pedido final');
  });

  it('CA-4-44 "Empezar a preparar" envía el avance con la versión vista y muestra los nuevos botones', async () => {
    const p = await abrir(admin());
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: '2 bultos', confirmado: false }));

    p.boton('Empezar a preparar')!.click();
    const peticion = http().expectOne(`${RUTA}/estado`);
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ nuevoEstado: 'EN_PREPARACION', comentario: '2 bultos', dniVerificado: null, version: 3 });
    peticion.flush(
      ok(admin({ estado: 'EN_PREPARACION', version: 5, transicionesPermitidas: [{ estado: 'LISTO_DESPACHO', etiqueta: 'Marcar listo para despacho' }] })),
    );
    await actualizar(p.harness);

    expect(p.boton('Marcar listo para despacho')).toBeDefined();
    expect(p.boton('Empezar a preparar')).toBeUndefined();
    expect(abrirDialogo.mock.calls[0][0].comentario?.obligatorio).toBe(false);
  });

  it('CA-4-46 "Marcar recogido" pide confirmar el DNI y manda dniVerificado', async () => {
    const p = await abrir(
      admin({
        estado: 'LISTO_RECOJO',
        modalidad: 'RECOJO',
        transicionesPermitidas: [
          { estado: 'ENTREGADO', etiqueta: 'Marcar recogido', requiereDni: true },
          { estado: 'DEVUELTO', etiqueta: 'Marcar no recogido', requiereComentario: true },
        ],
      }),
    );
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: null, confirmado: true }));

    p.boton('Marcar recogido')!.click();
    expect(abrirDialogo.mock.calls[0][0].confirmacion).toContain('DNI');
    const peticion = http().expectOne(`${RUTA}/estado`);
    expect(peticion.request.body).toMatchObject({ nuevoEstado: 'ENTREGADO', dniVerificado: true });
    peticion.flush(ok(admin({ estado: 'ENTREGADO', transicionesPermitidas: [], puedeCancelarStaff: false })));
  });

  it('CA-4-60 "Marcar no recogido" exige el motivo y pasa a DEVUELTO', async () => {
    const p = await abrir(
      admin({
        estado: 'LISTO_RECOJO',
        modalidad: 'RECOJO',
        transicionesPermitidas: [{ estado: 'DEVUELTO', etiqueta: 'Marcar no recogido', requiereComentario: true }],
      }),
    );
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: 'No lo recogió en 15 días', confirmado: false }));

    p.boton('Marcar no recogido')!.click();
    expect(abrirDialogo.mock.calls[0][0].comentario?.obligatorio).toBe(true);
    const peticion = http().expectOne(`${RUTA}/estado`);
    expect(peticion.request.body).toMatchObject({ nuevoEstado: 'DEVUELTO', comentario: 'No lo recogió en 15 días' });
    peticion.flush(ok(admin({ estado: 'DEVUELTO', transicionesPermitidas: [], puedeCancelarStaff: false })));
    await actualizar(p.harness);

    expect(p.el.textContent).toContain('Devuelto');
  });

  it('CA-4-48 si otro operador cambió el pedido avisa "El pedido cambió; recarga la página" y permite recargar', async () => {
    const p = await abrir(admin());
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: null, confirmado: false }));

    p.boton('Empezar a preparar')!.click();
    http()
      .expectOne(`${RUTA}/estado`)
      .flush(errorApi('CONFLICTO_VERSION', 'El pedido cambió; recarga la página'), { status: 409, statusText: 'Conflict' });
    await actualizar(p.harness);

    expect(p.el.querySelector('[role="alert"]')?.textContent).toContain('El pedido cambió; recarga la página');
    p.boton('Recargar')!.click();
    http().expectOne(RUTA).flush(ok(admin({ version: 7 })));
  });

  it('CA-4-47 una transición rechazada por la API muestra el motivo sin cambiar el pedido', async () => {
    const p = await abrir(admin());
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: null, confirmado: false }));

    p.boton('Empezar a preparar')!.click();
    http()
      .expectOne(`${RUTA}/estado`)
      .flush(errorApi('TRANSICION_INVALIDA', 'No se puede pasar el pedido de PAGADO a ENVIADO'), { status: 409, statusText: 'Conflict' });
    await actualizar(p.harness);

    expect(p.el.textContent).toContain('Pagado');
    expect(p.boton('Empezar a preparar')).toBeDefined();
  });

  it('CA-4-49 cancelar pide un motivo obligatorio y lo manda con la versión', async () => {
    const p = await abrir(admin());
    abrirDialogo.mockReturnValue(of<ResultadoAccionPedido>({ comentario: 'Error de stock', confirmado: false }));

    p.boton('Cancelar pedido')!.click();
    expect(abrirDialogo.mock.calls[0][0].comentario).toEqual({ etiqueta: 'Motivo', obligatorio: true, maximo: 300 });
    const peticion = http().expectOne(`${RUTA}/cancelar`);
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ motivo: 'Error de stock', version: 3 });
    peticion.flush(ok(admin({ estado: 'CANCELADO', transicionesPermitidas: [], puedeCancelarStaff: false })));
    await actualizar(p.harness);

    expect(p.el.textContent).toContain('Cancelado');
  });

  it('CA-4-49 si se cierra el diálogo sin confirmar no se llama a la API', async () => {
    const p = await abrir(admin());
    abrirDialogo.mockReturnValue(of(null));

    p.boton('Cancelar pedido')!.click();
    http().expectNone(`${RUTA}/cancelar`);
  });

  it('CA-4-50 desde LISTO_DESPACHO el botón de cancelar no aparece', async () => {
    const p = await abrir(admin({ estado: 'LISTO_DESPACHO', transicionesPermitidas: [], puedeCancelarStaff: false }));

    expect(p.boton('Cancelar pedido')).toBeUndefined();
  });

  it('CA-4-43 un pedido que no existe muestra "No encontramos ese pedido"', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        ...proveedores([{ path: 'pedidos/:id', component: DetallePedidoAdminPage }]),
        { provide: DialogoAccionPedidoService, useValue: { abrir: abrirDialogo } },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/pedidos/${ID}`, DetallePedidoAdminPage);
    harness.detectChanges();
    http().expectOne(RUTA).flush(errorApi('NO_ENCONTRADO', 'No existe'), { status: 404, statusText: 'Not Found' });
    await actualizar(harness);

    expect(harness.routeNativeElement?.textContent).toContain('No encontramos ese pedido');
  });
});

describe('Diálogo de acciones sobre el pedido', () => {
  function montar(datos: DatosAccionPedido) {
    const cerrar = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: MAT_DIALOG_DATA, useValue: datos },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    });
    const fixture = TestBed.createComponent(DialogoAccionPedido);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const aceptar = () => [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(datos.accion))!;
    return { fixture, el, cerrar, aceptar };
  }

  it('CA-4-60 el motivo obligatorio mantiene deshabilitada la acción hasta escribir al menos 3 caracteres', async () => {
    const d = montar({ titulo: 'No recogido', accion: 'Marcar no recogido', peligro: true, comentario: { etiqueta: 'Motivo', obligatorio: true, maximo: 300 } });
    expect(d.aceptar().disabled).toBe(true);

    const area = d.el.querySelector<HTMLTextAreaElement>('textarea')!;
    area.value = 'No vino';
    area.dispatchEvent(new Event('input'));
    d.fixture.detectChanges();
    expect(d.aceptar().disabled).toBe(false);
    d.aceptar().click();
    expect(d.cerrar).toHaveBeenCalledWith({ comentario: 'No vino', confirmado: false });
  });

  it('CA-4-46 la casilla del DNI es obligatoria para marcar recogido', async () => {
    const d = montar({ titulo: 'Recogido', accion: 'Marcar recogido', confirmacion: 'Verifiqué el DNI del destinatario' });
    expect(d.aceptar().disabled).toBe(true);

    const casilla = d.el.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
    casilla.click();
    d.fixture.detectChanges();
    d.aceptar().click();
    expect(d.cerrar).toHaveBeenCalledWith({ comentario: null, confirmado: true });
  });

  it('CA-4-44 el comentario del avance es opcional', async () => {
    const d = montar({ titulo: 'Preparar', accion: 'Empezar a preparar', comentario: { etiqueta: 'Comentario', obligatorio: false, maximo: 300 } });

    expect(d.aceptar().disabled).toBe(false);
    d.aceptar().click();
    expect(d.cerrar).toHaveBeenCalledWith({ comentario: null, confirmado: false });
  });
});
