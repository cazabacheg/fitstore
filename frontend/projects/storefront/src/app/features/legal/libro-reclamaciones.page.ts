import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SIN_TOAST, settingsApi } from 'shared';
import { ConfiguracionPublicaService } from 'shared/api/settings-service';
import { ConfirmacionReclamo } from './confirmacion-reclamo';
import { FormularioHojaReclamacion, type ReclamoEnviado } from './formulario-hoja-reclamacion';

/**
 * Libro de Reclamaciones virtual (P-3-03, `/libro-de-reclamaciones`): datos del proveedor y Hoja de Reclamación
 * del Código de Protección y Defensa del Consumidor. Público, con o sin cuenta.
 */
@Component({
  selector: 'fs-libro-reclamaciones',
  imports: [FormularioHojaReclamacion, ConfirmacionReclamo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fs-pagina libro">
      <h1>Libro de Reclamaciones</h1>
      @if (proveedor(); as p) {
        <section class="proveedor" data-testid="proveedor" aria-label="Datos del proveedor">
          <h2>Datos del proveedor</h2>
          <dl>
            <dt>Razón social</dt>
            <dd>{{ p.razonSocial }}</dd>
            <dt>RUC</dt>
            <dd>{{ p.ruc }}</dd>
            <dt>Dirección fiscal</dt>
            <dd>{{ p.direccionFiscal }}</dd>
          </dl>
        </section>
      }
      @if (enviado(); as e) {
        <fs-confirmacion-reclamo
          [numero]="e.numero"
          [fechaLimite]="e.fechaLimite"
          [email]="e.email"
        />
      } @else {
        <fs-formulario-hoja-reclamacion (enviado)="enviado.set($event)" />
      }
    </div>
  `,
  styles: `
    .libro {
      display: grid;
      gap: 24px;
      max-width: 900px;
      padding-block: 48px 96px;
    }
    .libro h1 {
      margin: 0;
    }
    .proveedor {
      padding: 20px;
      background: var(--fs-color-superficie-alterna);
      border-radius: var(--fs-radio-md, 8px);
    }
    .proveedor h2 {
      margin: 0 0 8px;
      font-size: 18px;
    }
    .proveedor dl {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 4px 16px;
      margin: 0;
    }
    .proveedor dt {
      color: var(--fs-color-texto-suave);
    }
    .proveedor dd {
      margin: 0;
    }
  `,
})
export class LibroReclamacionesPage {
  private readonly api = inject(ConfiguracionPublicaService);

  protected readonly proveedor = signal<settingsApi.Identidad | null>(null);
  protected readonly enviado = signal<ReclamoEnviado | null>(null);

  constructor() {
    this.api
      .obtenerConfiguracionPublica('body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (r) => this.proveedor.set(r.data?.identidad ?? null),
        error: () => this.proveedor.set(null), // el formulario se puede llenar aunque no lleguen los datos del proveedor
      });
  }
}
