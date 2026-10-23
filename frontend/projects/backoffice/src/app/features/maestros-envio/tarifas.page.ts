import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { type Observable, catchError, forkJoin, map, of } from 'rxjs';
import { PrecioPipe, SIN_TOAST, ToastService, UbigeoCatalogo, comoApiError, deliveryApi, type Pagina, type settingsApi } from 'shared';
import { EnviosService, TarifasService } from 'shared/api/delivery-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoError } from '../../ui/estados/estado-error';
import { SelectorUbigeo } from '../../ui/selector-ubigeo/selector-ubigeo';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { DialogoTarifa, type DatosDialogoTarifa } from './dialogo-tarifa';

/** Una tarifa con el nombre de su destino ya resuelto (el frontend lo pide a settings, sin llamadas extra a delivery). */
type Fila = deliveryApi.Tarifa & { destino: string };

const PRECIO = new PrecioPipe();
const NIVEL: Record<string, string> = {
  DEPARTAMENTO: 'departamento',
  PROVINCIA: 'provincia',
  DISTRITO: 'distrito',
};

/** "Arequipa (departamento)", "Lima › Cañete (provincia)", "Lima › Lima › Miraflores". */
export function nombreDestino(
  tarifa: deliveryApi.Tarifa,
  n: settingsApi.UbigeoNombre | null,
): string {
  if (!n) return tarifa.ubigeo;
  switch (tarifa.nivel) {
    case 'DEPARTAMENTO':
      return `${n.nombre} (departamento)`;
    case 'PROVINCIA':
      return `${n.departamento ?? ''} › ${n.nombre} (provincia)`;
    default:
      return `${n.departamento ?? ''} › ${n.provincia ?? ''} › ${n.nombre}`;
  }
}

function plazo(t: deliveryApi.Tarifa): string {
  return t.plazoDiasMin === t.plazoDiasMax
    ? `${t.plazoDiasMax} ${t.plazoDiasMax === 1 ? 'día' : 'días'}`
    : `${t.plazoDiasMin}–${t.plazoDiasMax} días`;
}

/**
 * Tarifas (P-6-05, `/tarifas`, ADMIN): pestañas Delivery local y Agencia, diálogo con el selector de ubigeo con nivel
 * (E2) y "Probar cotización" (E3, CA-6-07 a CA-6-11). El destino abre el diálogo de edición (R11).
 */
@Component({
  selector: 'fs-tarifas',
  imports: [EncabezadoPagina, FsTabla, FsCelda, EstadoError, SelectorUbigeo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .fs-pestanas {
      display: flex;
      gap: 4px;
      margin: 16px 0;
      border-bottom: 1px solid var(--fs-color-linea);
    }
    .fs-pestana {
      padding: 8px 16px;
      border: 0;
      border-bottom: 2px solid transparent;
      background: none;
      color: var(--fs-color-texto-suave);
      font: inherit;
      cursor: pointer;
    }
    .fs-pestana--activa {
      color: var(--fs-color-texto);
      border-bottom-color: var(--fs-color-primario-texto);
      font-weight: 600;
    }
    .probar {
      display: grid;
      gap: 12px;
      max-width: 640px;
      margin: 24px 0;
      padding: 16px 20px;
      border: 1px solid var(--fs-color-linea);
      border-radius: 8px;
    }
    .probar h2 {
      margin: 0;
      font-size: 16px;
    }
  `,
  template: `
    <fs-encabezado-pagina titulo="Tarifas">
      <button accion type="button" class="fs-btn fs-btn--primario" (click)="nueva()">
        Nueva tarifa
      </button>
    </fs-encabezado-pagina>
    <div class="fs-pestanas" role="tablist" aria-label="Modalidad de la tarifa">
      <button
        type="button"
        role="tab"
        class="fs-pestana"
        [class.fs-pestana--activa]="modalidad() === 'DELIVERY_LOCAL'"
        [attr.aria-selected]="modalidad() === 'DELIVERY_LOCAL'"
        (click)="cambiarModalidad('DELIVERY_LOCAL')"
      >
        Delivery local
      </button>
      <button
        type="button"
        role="tab"
        class="fs-pestana"
        [class.fs-pestana--activa]="modalidad() === 'AGENCIA'"
        [attr.aria-selected]="modalidad() === 'AGENCIA'"
        (click)="cambiarModalidad('AGENCIA')"
      >
        Agencia
      </button>
    </div>
    @if (error()) {
      <fs-estado-error texto="No pudimos cargar las tarifas." (reintentar)="cargar()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="pagina()"
        [cargando]="cargando()"
        columnaId="destino"
        [abribleEnDialogo]="true"
        [acciones]="acciones"
        tituloVacio="Sin tarifas"
        textoVacio="Crea una tarifa para esta modalidad."
        (alAbrir)="editar($event)"
      >
        <ng-template fsCelda="activa" let-fila>
          <span
            class="fs-insignia"
            [class]="'fs-insignia fs-insignia--' + (fila.activa ? 'exito' : 'neutra')"
            >{{ fila.activa ? 'Activa' : 'Inactiva' }}</span
          >
        </ng-template>
      </fs-tabla>
    }

    <section class="probar" aria-label="Probar cotización">
      <h2>Probar cotización</h2>
      <fs-selector-ubigeo [(valor)]="ubigeoPrueba" />
      <div>
        <button
          type="button"
          class="fs-btn fs-btn--contorno"
          [disabled]="!ubigeoPrueba() || probando()"
          (click)="probar()"
        >
          Probar con {{ modalidad() === 'AGENCIA' ? 'agencia' : 'delivery local' }}
        </button>
      </div>
      @if (resultado(); as r) {
        <p data-testid="resultado-cotizacion" [class]="r.cobertura ? '' : 'fs-suave'">
          {{ r.texto }}
        </p>
      }
    </section>
  `,
})
export class TarifasPage {
  private readonly api = inject(TarifasService);
  private readonly envios = inject(EnviosService);
  private readonly catalogo = inject(UbigeoCatalogo);
  private readonly dialogos = inject(MatDialog);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected readonly modalidad = signal<deliveryApi.ModalidadEntrega>('DELIVERY_LOCAL');
  protected readonly filas = signal<Fila[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly ubigeoPrueba = signal<string | null>(null);
  protected readonly probando = signal(false);
  protected readonly resultado = signal<{ texto: string; cobertura: boolean } | null>(null);

  protected readonly pagina = computed<Pagina<Fila>>(() => ({
    contenido: this.filas(),
    pagina: 0,
    tamano: Math.max(this.filas().length, 20),
    totalElementos: this.filas().length,
    totalPaginas: 1,
  }));

  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'destino', titulo: 'Destino', valor: (t) => t.destino },
    { clave: 'nivel', titulo: 'Nivel', valor: (t) => NIVEL[t.nivel] ?? t.nivel },
    { clave: 'costo', titulo: 'Costo', valor: (t) => PRECIO.transform(t.costo) },
    { clave: 'plazo', titulo: 'Plazo', valor: (t) => plazo(t) },
    { clave: 'activa', titulo: 'Estado' },
  ];
  protected readonly acciones = (fila: Fila): AccionFila<Fila>[] => [
    fila.activa
      ? { etiqueta: 'Desactivar', alEjecutar: (t) => this.cambiarActiva(t, false) }
      : { etiqueta: 'Activar', alEjecutar: (t) => this.cambiarActiva(t, true) },
    { etiqueta: 'Eliminar', alEjecutar: (t) => this.eliminar(t) },
  ];

  constructor() {
    this.cargar();
  }

  protected cambiarModalidad(modalidad: deliveryApi.ModalidadEntrega): void {
    this.modalidad.set(modalidad);
    this.resultado.set(null);
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api
      .listarTarifas(this.modalidad(), undefined, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: (r) => this.resolverNombres(r.data ?? []),
        error: () => {
          this.cargando.set(false);
          this.error.set(true);
        },
      });
  }

  /** Los destinos se piden a settings (con la caché de ubigeo); si uno falla, se muestra su código. */
  private resolverNombres(tarifas: deliveryApi.Tarifa[]): void {
    if (tarifas.length === 0) {
      this.filas.set([]);
      this.cargando.set(false);
      return;
    }
    forkJoin(
      tarifas.map((t) =>
        this.catalogo.nombre(t.ubigeo).pipe(
          map((n) => nombreDestino(t, n)),
          catchError(() => of(t.ubigeo)),
        ),
      ),
    ).subscribe((nombres) => {
      this.filas.set(tarifas.map((t, i) => ({ ...t, destino: nombres[i] })));
      this.cargando.set(false);
    });
  }

  protected nueva(): void {
    this.abrirDialogo({ modalidad: this.modalidad() }, 'Tarifa creada');
  }

  protected editar(fila: Fila): void {
    this.abrirDialogo({ tarifa: fila, modalidad: fila.modalidad }, 'Guardado');
  }

  private abrirDialogo(datos: DatosDialogoTarifa, aviso: string): void {
    this.dialogos
      .open<DialogoTarifa, DatosDialogoTarifa, deliveryApi.Tarifa | undefined>(DialogoTarifa, {
        data: datos,
        width: '560px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((guardada) => {
        if (!guardada) return;
        this.toasts.mostrar('exito', aviso);
        if (guardada.modalidad !== this.modalidad()) this.modalidad.set(guardada.modalidad);
        this.cargar();
      });
  }

  private cambiarActiva(fila: Fila, activa: boolean): void {
    this.api
      .editarTarifa(fila.id, {
        modalidad: fila.modalidad,
        ubigeo: fila.ubigeo,
        costo: fila.costo,
        plazoDiasMin: fila.plazoDiasMin,
        plazoDiasMax: fila.plazoDiasMax,
        activa,
      })
      .subscribe({
        next: () => {
          this.toasts.mostrar('exito', activa ? 'Tarifa activada' : 'Tarifa desactivada');
          this.cargar();
        },
        error: () => this.cargar(),
      });
  }

  private eliminar(fila: Fila): void {
    this.confirmacion
      .confirmar({
        titulo: '¿Eliminar esta tarifa?',
        mensaje: `${fila.destino}. Los pedidos ya creados conservan su costo; las cotizaciones nuevas dejan de usarla.`,
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.eliminarTarifa(fila.id).subscribe({
          next: () => {
            this.toasts.mostrar('exito', 'Tarifa eliminada');
            this.cargar();
          },
          error: () => this.cargar(),
        });
      });
  }

  /** "Probar cotización" (E3, CA-6-11): qué tarifa se aplicaría a un distrito o "Sin cobertura". */
  protected probar(): void {
    const ubigeo = this.ubigeoPrueba();
    if (!ubigeo) return;
    this.probando.set(true);
    const sinAviso = { context: new HttpContext().set(SIN_TOAST, true) };
    (
      this.envios.cotizarEnvio(
        { modalidad: this.modalidad(), ubigeo },
        'body',
        false,
        sinAviso,
      ) as Observable<deliveryApi.RespuestaCotizacionEnvio>
    ).subscribe({
      next: (r) => {
        this.probando.set(false);
        const c = r.data;
        this.resultado.set(
          c
            ? {
                cobertura: true,
                texto: `Se aplica la tarifa de ${NIVEL[c.nivelTarifa] ?? c.nivelTarifa} (${c.ubigeoTarifa}): ${PRECIO.transform(c.costo)} en ${plazo({ plazoDiasMin: c.plazoDiasMin, plazoDiasMax: c.plazoDiasMax } as deliveryApi.Tarifa)}.`,
              }
            : null,
        );
      },
      error: (error: unknown) => {
        this.probando.set(false);
        const cuerpo = comoApiError(error);
        this.resultado.set({
          cobertura: false,
          texto:
            cuerpo?.codigo === 'SIN_COBERTURA'
              ? 'Sin cobertura: no hay tarifa activa para este destino y modalidad.'
              : (cuerpo?.mensaje ?? 'No pudimos probar la cotización.'),
        });
      },
    });
  }
}
