import { Injectable, signal } from '@angular/core';

/** Estado de la plataforma visto desde el navegador: `noDisponible` enciende el banner "Servicio no disponible". */
@Injectable({ providedIn: 'root' })
export class EstadoServicio {
  readonly noDisponible = signal(false);
}
