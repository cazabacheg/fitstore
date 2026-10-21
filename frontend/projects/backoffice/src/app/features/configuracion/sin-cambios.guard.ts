import { inject } from '@angular/core';
import type { CanDeactivateFn } from '@angular/router';
import { DialogoService } from '../../ui/dialogo/dialogo.service';

/** Pantalla que sabe si tiene cambios sin guardar. */
export interface ConCambiosSinGuardar {
  haySinGuardar(): boolean;
}

/** Al salir de la pantalla con cambios sin guardar, pregunta antes de descartarlos (P-3-04). */
export const sinCambiosGuard: CanDeactivateFn<ConCambiosSinGuardar> = (pantalla) =>
  !pantalla.haySinGuardar() ||
  inject(DialogoService).confirmar({
    titulo: '¿Salir sin guardar?',
    mensaje: 'Tienes cambios sin guardar. Si sales ahora, se perderán.',
    accion: 'Salir sin guardar',
    peligro: true,
  });
