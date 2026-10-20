import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatMenuModule } from '@angular/material/menu';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, filter, map, of, startWith } from 'rxjs';
import { SIN_TOAST, SesionStore, authApi } from 'shared';
import { migasDe } from '../core/migas';

/** Barra superior del backoffice (P-0b-02): migas y menú de usuario (perfil, cambiar contraseña, salir). */
@Component({
  selector: 'fs-barra-superior',
  imports: [MatMenuModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './barra-superior.html',
  styleUrl: './barra-superior.css',
})
export class BarraSuperior {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionStore);
  private readonly auth = inject(authApi.AutenticacionService);
  private readonly staffPropio = inject(authApi.StaffPropioService);

  /** Muestra el botón ☰ (menú en tablet); con el menú fijo no hace falta. */
  readonly conBotonMenu = input(false);
  readonly menuSolicitado = output<void>();

  protected readonly migas = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      startWith(null),
      map(() => migasDe(this.router.routerState.snapshot.root)),
    ),
    { initialValue: [] as string[] },
  );

  private readonly perfil = signal<authApi.Staff | null>(null);
  protected readonly nombre = computed(() => {
    const p = this.perfil() ?? this.sesion.cuenta();
    return p ? `${p.nombres} ${p.apellidos}`.trim() : '';
  });
  protected readonly iniciales = computed(() => {
    const p = this.perfil() ?? this.sesion.cuenta();
    return p ? `${p.nombres.charAt(0)}${p.apellidos.charAt(0)}`.toUpperCase() : '';
  });

  constructor() {
    // El nombre sale de /staff/me; si falla se queda el de la sesión, sin avisos ni bloqueo.
    this.staffPropio
      .obtenerMiPerfilStaff('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .pipe(
        takeUntilDestroyed(),
        catchError(() => EMPTY),
      )
      .subscribe((respuesta) => this.perfil.set(respuesta.data ?? null));
  }

  /** Cierra la sesión: avisa al servidor y, falle o no, borra la sesión local y va al login. */
  protected salir(): void {
    this.auth
      .cerrarSesion('body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.sesion.cerrarSesion();
        void this.router.navigateByUrl('/ingresar');
      });
  }
}
