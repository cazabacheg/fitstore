import { Injectable, computed, signal } from '@angular/core';
import type { Sesion } from '../api/auth-service';

/**
 * Estado de la sesión (R6). El access token vive solo en memoria: nunca se guarda en localStorage ni sessionStorage
 * (auth 4.4). Al recargar la página, la sesión se recupera con el refresh por cookie.
 */
@Injectable({ providedIn: 'root' })
export class SesionStore {
  private readonly sesion = signal<Sesion | null>(null);

  readonly cuenta = computed(() => this.sesion()?.cuenta ?? null);
  readonly rol = computed(() => this.cuenta()?.rol ?? null);
  readonly tipo = computed(() => this.cuenta()?.tipo ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  /** Access token actual, o `null` sin sesión. */
  token(): string | null {
    return this.sesion()?.accessToken ?? null;
  }

  /** La llaman las pantallas de login de la spec 1 y el refresco de sesión. */
  iniciarSesion(sesion: Sesion): void {
    this.sesion.set(sesion);
  }

  /** Borra la sesión local (la llamada a `/auth/logout` la hace la pantalla que cierra la sesión). */
  cerrarSesion(): void {
    this.sesion.set(null);
  }
}
