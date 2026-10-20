import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { AutenticacionService, type Sesion } from '../api/auth-service';
import { SesionStore } from './sesion.store';

/**
 * Renueva el access token con `POST /auth/refresh` (cookie `fs_refresh`).
 * Si ya hay un refresco en curso, todos los que lo pidan comparten el mismo (CA-0b-10).
 */
@Injectable({ providedIn: 'root' })
export class RefrescoSesion {
  private readonly api = inject(AutenticacionService);
  private readonly store = inject(SesionStore);
  private enCurso: Observable<Sesion> | null = null;

  refrescar(): Observable<Sesion> {
    this.enCurso ??= this.api.refrescarSesion().pipe(
      map((respuesta) => {
        if (!respuesta.data) throw new Error('El refresco de sesión no devolvió datos');
        return respuesta.data;
      }),
      tap((sesion) => this.store.iniciarSesion(sesion)),
      finalize(() => (this.enCurso = null)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.enCurso;
  }
}
