import {
  ApplicationRef,
  EnvironmentInjector,
  Injectable,
  createComponent,
  inject,
} from '@angular/core';
import { authApi } from 'shared';
import { DialogoDireccion } from './dialogo-direccion';

/**
 * Abre `DialogoDireccion` desde cualquier pantalla sin que esta lleve el diálogo en su plantilla (A6): lo usa la
 * libreta de direcciones y lo usará el checkout (SPEC-4) para agregar una dirección sin salir de la página.
 */
@Injectable({ providedIn: 'root' })
export class DialogoDireccionService {
  private readonly injector = inject(EnvironmentInjector);
  private readonly aplicacion = inject(ApplicationRef);

  /**
   * Con `direccion` la edita (PUT); sin ella crea una (POST).
   *
   * @returns la dirección guardada, o `undefined` si se cancela
   */
  abrir(direccion?: authApi.Direccion): Promise<authApi.Direccion | undefined> {
    return new Promise((resolver) => {
      const ref = createComponent(DialogoDireccion, { environmentInjector: this.injector });
      ref.setInput('direccion', direccion ?? null);
      this.aplicacion.attachView(ref.hostView);
      document.body.appendChild(ref.location.nativeElement as HTMLElement);
      const cerrar = (resultado: authApi.Direccion | undefined) => {
        ref.destroy();
        resolver(resultado);
      };
      ref.instance.guardada.subscribe((guardada) => cerrar(guardada));
      ref.instance.cancelada.subscribe(() => cerrar(undefined));
    });
  }
}
