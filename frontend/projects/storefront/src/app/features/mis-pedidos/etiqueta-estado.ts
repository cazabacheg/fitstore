import { Component, computed, input } from '@angular/core';
import { FsBadge, type VarianteBadge } from '../../ui/badge/badge';

interface DatosEstado {
  etiqueta: string;
  variante: VarianteBadge;
}

/** Texto y color de cada estado del pedido para el cliente (contrato visual compartido con el backoffice). */
export const ESTADOS_PEDIDO: Record<string, DatosEstado> = {
  PENDIENTE_PAGO: { etiqueta: 'Pendiente de pago', variante: 'aviso' },
  PAGADO: { etiqueta: 'Pagado', variante: 'exito' },
  EN_PREPARACION: { etiqueta: 'En preparación', variante: 'info' },
  LISTO_DESPACHO: { etiqueta: 'En preparación', variante: 'info' },
  LISTO_RECOJO: { etiqueta: 'Listo para recoger', variante: 'info' },
  ENVIADO: { etiqueta: 'Enviado', variante: 'info' },
  ENTREGADO: { etiqueta: 'Entregado', variante: 'exito' },
  CANCELADO: { etiqueta: 'Cancelado', variante: 'neutra' },
  EXPIRADO: { etiqueta: 'Expirado', variante: 'neutra' },
  DEVUELTO: { etiqueta: 'Devuelto a la tienda', variante: 'error' },
};

/** Insignia del estado de un pedido. */
@Component({
  selector: 'fs-etiqueta-estado',
  imports: [FsBadge],
  template: `<fs-badge [variante]="datos().variante">{{ datos().etiqueta }}</fs-badge>`,
})
export class EtiquetaEstado {
  readonly estado = input.required<string>();
  protected readonly datos = computed(
    () => ESTADOS_PEDIDO[this.estado()] ?? { etiqueta: this.estado(), variante: 'neutra' as const },
  );
}
