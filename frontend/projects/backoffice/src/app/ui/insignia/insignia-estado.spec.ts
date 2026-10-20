import { TestBed } from '@angular/core/testing';
import { deliveryApi, orderApi, paymentApi } from 'shared';
import { InsigniaEstado } from './insignia-estado';
import { ESTADOS, insigniaDe } from './estados';

describe('InsigniaEstado (ui)', () => {
  it.each([
    ['pedido', Object.values(orderApi.EstadoPedido)],
    ['pago', [...Object.values(paymentApi.EstadoIntencion), ...Object.values(paymentApi.EstadoTransaccion)]],
    ['entrega', Object.values(deliveryApi.EstadoEntrega)],
    ['reembolso', Object.values(paymentApi.EstadoReembolso)],
  ] as const)('CA-0b-25 cada estado de %s del contrato tiene etiqueta y variante', (tipo, codigos) => {
    for (const codigo of codigos) {
      const insignia = ESTADOS[tipo][codigo as string];
      expect(insignia, `${tipo} ${codigo}`).toBeDefined();
      expect(insignia.etiqueta).not.toMatch(/_|^[A-Z]+$/);
    }
  });

  it('CA-0b-25 muestra el estado en lenguaje de la persona y con la variante semántica', () => {
    const f = TestBed.createComponent(InsigniaEstado);
    f.componentRef.setInput('tipo', 'pedido');
    f.componentRef.setInput('estado', 'PENDIENTE_PAGO');
    f.detectChanges();
    const span = (f.nativeElement as HTMLElement).querySelector('.fs-insignia') as HTMLElement;
    expect(span.textContent?.trim()).toBe('Pendiente de pago');
    expect(span.classList.contains('fs-insignia--aviso')).toBe(true);
  });

  it('CA-0b-25 las variantes siguen la ficha del sistema de diseño', () => {
    expect(insigniaDe('pedido', 'PAGADO')).toEqual({ etiqueta: 'Pagado', variante: 'exito' });
    expect(insigniaDe('pedido', 'EN_PREPARACION').variante).toBe('info');
    expect(insigniaDe('pedido', 'CANCELADO').variante).toBe('neutra');
    expect(insigniaDe('pedido', 'DEVUELTO').variante).toBe('error');
    expect(insigniaDe('reembolso', 'FALLIDO')).toEqual({ etiqueta: 'Reembolso fallido', variante: 'error' });
    expect(insigniaDe('pago', 'REEMBOLSADA')).toEqual({ etiqueta: 'Reembolsado', variante: 'marca' });
    expect(insigniaDe('pago', 'RECHAZADA')).toEqual({ etiqueta: 'Pago rechazado', variante: 'error' });
  });

  it('CA-0b-25 un estado desconocido se muestra neutro con su código legible, sin romper', () => {
    expect(insigniaDe('pedido', 'EN_REVISION')).toEqual({ etiqueta: 'En revision', variante: 'neutra' });
  });
});
