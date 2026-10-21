import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { type Observable, map, of, switchMap } from 'rxjs';
import { deliveryApi } from 'shared';
import { EntregasService } from 'shared/api/delivery-service';
import {
  DialogoAsignar,
  DialogoEnvioAgencia,
  DialogoFallida,
  type DatosDialogoEntrega,
} from './dialogos-entrega';

/**
 * Lleva una entrega al siguiente paso desde el listado o el detalle. Los pasos con datos (asignar, reprogramar,
 * no se pudo entregar, registrar el envío) abren su diálogo; los demás llaman a la API con la versión que vio el
 * operador (E6). Emite la entrega actualizada, o `undefined` si el operador cerró el diálogo.
 */
@Injectable({ providedIn: 'root' })
export class AvanceEntrega {
  private readonly api = inject(EntregasService);
  private readonly dialogos = inject(MatDialog);

  /** Desde una fila del listado: la fila no trae la versión, así que primero se lee el detalle. */
  ejecutarPorId(id: string, accion: string): Observable<deliveryApi.Entrega | undefined> {
    return this.api.obtenerEntrega(id).pipe(
      map((r) => r.data as deliveryApi.Entrega),
      switchMap((entrega) => this.ejecutar(entrega, accion)),
    );
  }

  ejecutar(
    entrega: deliveryApi.Entrega,
    accion: string,
  ): Observable<deliveryApi.Entrega | undefined> {
    const version = { version: entrega.version };
    switch (accion) {
      case 'ASIGNAR':
      case 'REPROGRAMAR':
        return this.abrir(DialogoAsignar, entrega);
      case 'FALLIDA':
        return this.abrir(DialogoFallida, entrega);
      case 'ENVIADA_AGENCIA':
        return this.abrir(DialogoEnvioAgencia, entrega);
      case 'EN_RUTA':
        return this.api.marcarEnRuta(entrega.id, version).pipe(map((r) => r.data));
      case 'ENTREGADA':
        return this.api.marcarEntregada(entrega.id, version).pipe(map((r) => r.data));
      case 'DISPONIBLE_AGENCIA':
        return this.api.marcarDisponibleAgencia(entrega.id, version).pipe(map((r) => r.data));
      default:
        return of(undefined);
    }
  }

  private abrir(
    componente: new (...args: never[]) => unknown,
    entrega: deliveryApi.Entrega,
  ): Observable<deliveryApi.Entrega | undefined> {
    return this.dialogos
      .open<unknown, DatosDialogoEntrega, deliveryApi.Entrega | undefined>(componente, {
        data: { entrega },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed();
  }
}
