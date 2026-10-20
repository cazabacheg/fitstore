import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { BarraSuperior } from './barra-superior';
import { MenuLateral } from './menu-lateral';

/** Shell del backoffice (P-0b-02): menú lateral por rol, barra superior y contenido. */
@Component({
  selector: 'fs-backoffice-shell',
  imports: [MatSidenavModule, RouterOutlet, MenuLateral, BarraSuperior],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-sidenav-container class="fs-shell">
      <mat-sidenav
        class="fs-shell__lateral"
        [mode]="escritorio() ? 'side' : 'over'"
        [opened]="escritorio() || menuAbierto()"
        (closedStart)="menuAbierto.set(false)"
      >
        <fs-menu-lateral />
      </mat-sidenav>
      <mat-sidenav-content class="fs-bo__principal">
        <fs-barra-superior
          [conBotonMenu]="!escritorio()"
          (menuSolicitado)="menuAbierto.set(true)"
        />
        <main class="fs-bo__contenido"><router-outlet /></main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styleUrl: './backoffice-shell.css',
})
export class BackofficeShell {
  /** Desde 1024 px el menú va fijo; en tablet se abre encima con el botón ☰. */
  protected readonly escritorio = toSignal(
    inject(BreakpointObserver)
      .observe('(min-width: 1024px)')
      .pipe(map((estado) => estado.matches)),
    { initialValue: true },
  );
  protected readonly menuAbierto = signal(false);

  constructor() {
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.menuAbierto.set(false));
  }
}
