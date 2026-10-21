import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { map, type Observable } from 'rxjs';
import { SIN_TOAST, ToastService, deliveryApi, type Pagina } from 'shared';
import { RepartidoresService } from 'shared/api/delivery-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { DialogoRepartidor } from './dialogo-repartidor';

type Fila = deliveryApi.Repartidor;

const VEHICULOS: Record<string, string> = { MOTO: 'Moto', AUTO: 'Auto', BICICLETA: 'Bicicleta' };

/**
 * Repartidores (P-6-04, `/repartidores`): el nombre abre el diálogo de edición (sin página de detalle) y las acciones
 * son Desactivar o Activar y Eliminar, esta última solo si no tiene entregas (R11, CA-6-16 a CA-6-18).
 */
@Component({
  selector: 'fs-repartidores',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-encabezado-pagina titulo="Repartidores">
      <button accion type="button" class="fs-btn fs-btn--primario" (click)="registrar()">
        Registrar repartidor
      </button>
    </fs-encabezado-pagina>
    <fs-barra-filtros placeholder="Buscar por nombre o placa" [selects]="selects" />
    @if (error()) {
      <fs-estado-error texto="No pudimos cargar los repartidores." (reintentar)="cargar()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="pagina()"
        [cargando]="cargando()"
        columnaId="nombres"
        [abribleEnDialogo]="true"
        [acciones]="acciones"
        tituloVacio="Sin repartidores"
        textoVacio="Registra a tu primer repartidor o prueba con otros filtros."
        (alAbrir)="editar($event)"
      >
        <ng-template fsCelda="activo" let-fila>
          <span
            class="fs-insignia"
            [class]="'fs-insignia fs-insignia--' + (fila.activo ? 'exito' : 'neutra')"
            >{{ fila.activo ? 'Activo' : 'Inactivo' }}</span
          >
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class RepartidoresPage {
  private readonly api = inject(RepartidoresService);
  private readonly dialogos = inject(MatDialog);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);
  private readonly filtros = toSignal(inject(ActivatedRoute).queryParamMap, {
    initialValue: inject(ActivatedRoute).snapshot.queryParamMap,
  });

  protected readonly selects: SelectFiltro[] = [
    {
      clave: 'estado',
      etiqueta: 'Estado',
      opciones: [
        { valor: 'ACTIVO', etiqueta: 'Activo' },
        { valor: 'INACTIVO', etiqueta: 'Inactivo' },
      ],
    },
  ];
  protected readonly columnas: ColumnaTabla<Fila>[] = [
    { clave: 'nombres', titulo: 'Nombre', valor: (r) => r.nombres },
    { clave: 'telefono', titulo: 'Celular', valor: (r) => r.telefono },
    {
      clave: 'vehiculo',
      titulo: 'Vehículo',
      valor: (r) => `${VEHICULOS[r.vehiculo] ?? r.vehiculo}${r.placa ? ' · ' + r.placa : ''}`,
    },
    { clave: 'activo', titulo: 'Estado' },
  ];
  protected readonly acciones = (fila: Fila): AccionFila<Fila>[] => [
    fila.activo
      ? { etiqueta: 'Desactivar', alEjecutar: (r) => this.desactivar(r) }
      : {
          etiqueta: 'Activar',
          alEjecutar: (r) => this.cambiar(this.api.activarRepartidor(r.id), 'Repartidor activado'),
        },
    ...(fila.tieneEntregas
      ? []
      : [{ etiqueta: 'Eliminar', alEjecutar: (r: Fila) => this.eliminar(r) }]),
  ];

  protected readonly todos = signal<Fila[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly pagina = computed<Pagina<Fila>>(() => {
    const q = (this.filtros().get('q') ?? '').toLowerCase();
    const estado = this.filtros().get('estado');
    const filas = this.todos().filter(
      (r) =>
        (!q || `${r.nombres} ${r.placa ?? ''}`.toLowerCase().includes(q)) &&
        (!estado || r.activo === (estado === 'ACTIVO')),
    );
    return {
      contenido: filas,
      pagina: 0,
      tamano: Math.max(filas.length, 20),
      totalElementos: filas.length,
      totalPaginas: 1,
    };
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api
      .listarRepartidores(undefined, 'body', false, {
        context: new HttpContext().set(SIN_TOAST, true),
      })
      .subscribe({
        next: (r) => {
          this.todos.set(r.data ?? []);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
          this.error.set(true);
        },
      });
  }

  protected registrar(): void {
    this.abrirDialogo(undefined, 'Repartidor registrado');
  }

  protected editar(repartidor: Fila): void {
    this.abrirDialogo(repartidor, 'Guardado');
  }

  private abrirDialogo(repartidor: Fila | undefined, aviso: string): void {
    this.dialogos
      .open<DialogoRepartidor, { repartidor?: Fila }, Fila | undefined>(DialogoRepartidor, {
        data: { repartidor },
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (!guardado) return;
        this.toasts.mostrar('exito', aviso);
        this.cargar();
      });
  }

  private desactivar(r: Fila): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Desactivar a ${r.nombres}?`,
        mensaje:
          'Ya no aparecerá para asignar entregas, pero sus entregas anteriores lo conservan.',
        accion: 'Desactivar',
        peligro: true,
      })
      .subscribe((ok) => {
        if (ok) this.cambiar(this.api.desactivarRepartidor(r.id), 'Repartidor desactivado');
      });
  }

  private eliminar(r: Fila): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Eliminar a ${r.nombres}?`,
        mensaje: 'Se borra del todo. Si ya tiene entregas, mejor desactívalo.',
        accion: 'Eliminar',
        peligro: true,
      })
      .subscribe((ok) => {
        if (ok)
          this.cambiar(
            this.api.eliminarRepartidor(r.id).pipe(map(() => undefined)),
            'Repartidor eliminado',
          );
      });
  }

  /** Aplica el cambio; si la API lo rechaza (409 con su motivo) el interceptor lo muestra y se vuelve a leer la lista. */
  private cambiar(llamada: Observable<unknown>, aviso: string): void {
    llamada.subscribe({
      next: () => {
        this.toasts.mostrar('exito', aviso);
        this.cargar();
      },
      error: () => this.cargar(),
    });
  }
}
