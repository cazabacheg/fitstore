import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { DialogoConfirmacion, type DatosConfirmacion } from './dialogo-confirmacion';

/** Abre diálogos del backoffice con los tokens de la marca (`MatDialog`). */
@Injectable({ providedIn: 'root' })
export class DialogoService {
  private readonly dialogo = inject(MatDialog);

  /** Pregunta antes de una acción destructiva o irreversible. Emite `true` si se confirma; `Esc` o "Volver" dan `false`. */
  confirmar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogo
      .open<DialogoConfirmacion, DatosConfirmacion, boolean>(DialogoConfirmacion, {
        data: datos,
        maxWidth: '440px',
        width: '100%',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
      })
      .afterClosed()
      .pipe(map((resultado) => resultado === true));
  }
}
