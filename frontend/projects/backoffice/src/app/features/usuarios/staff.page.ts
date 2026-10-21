import { HttpContext } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import {
  BehaviorSubject,
  EMPTY,
  catchError,
  combineLatest,
  switchMap,
  tap,
  type Observable,
} from 'rxjs';
import { SIN_TOAST, ToastService, authApi, type ConsultaPagina, type Pagina } from 'shared';
import { StaffService } from 'shared/api/auth-service';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../../ui/tabla/fs-tabla';
import { DialogoStaff, type DatosDialogoStaff } from './dialogo-staff';
import { ETIQUETA_ESTADO_STAFF, ETIQUETA_ROL, fechaHora } from './formato';

type Miembro = authApi.Staff;

const VARIANTE_ESTADO: Record<string, string> = {
  ACTIVO: 'exito',
  INACTIVO: 'error',
  INVITACION_PENDIENTE: 'aviso',
};

/** Staff (P-1-08, ADMIN): tabla paginada con filtros, y diálogo para invitar y editar. El nombre abre el diálogo (R11). */
@Component({
  selector: 'fs-staff-page',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError],
  template: `
    <fs-encabezado-pagina titulo="Staff">
      <button accion type="button" class="fs-btn fs-btn--primario" (click)="invitar()">
        Invitar miembro
      </button>
    </fs-encabezado-pagina>
    <fs-barra-filtros placeholder="Buscar por nombre o correo" [selects]="selects" />
    @if (error()) {
      <fs-estado-error titulo="No pudimos cargar el personal" (reintentar)="recargar()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="cargando()"
        columnaId="nombres"
        [abribleEnDialogo]="true"
        [acciones]="acciones"
        tituloVacio="Sin miembros"
        textoVacio="Prueba con otros filtros."
        (alAbrir)="editar($event)"
        (consulta)="consulta.next($event)"
      >
        <ng-template fsCelda="estado" let-fila>
          <span class="fs-insignia" [class]="'fs-insignia fs-insignia--' + variante(fila)">{{
            etiquetaEstado(fila)
          }}</span>
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class StaffPage {
  private readonly api = inject(StaffService);
  private readonly dialogos = inject(MatDialog);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly selects: SelectFiltro[] = [
    {
      clave: 'rol',
      etiqueta: 'Rol',
      opciones: [
        { valor: 'ADMIN', etiqueta: 'Admin' },
        { valor: 'OPERADOR', etiqueta: 'Operador' },
      ],
    },
    {
      clave: 'estado',
      etiqueta: 'Estado',
      opciones: [
        { valor: 'ACTIVO', etiqueta: 'Activo' },
        { valor: 'INVITACION_PENDIENTE', etiqueta: 'Invitación pendiente' },
        { valor: 'INACTIVO', etiqueta: 'Inactivo' },
      ],
    },
  ];

  protected readonly columnas: ColumnaTabla<Miembro>[] = [
    {
      clave: 'nombres',
      titulo: 'Nombre',
      valor: (s) => `${s.nombres} ${s.apellidos}`,
      ordenable: true,
    },
    { clave: 'email', titulo: 'Correo', valor: (s) => s.email, ordenable: true },
    { clave: 'rol', titulo: 'Rol', valor: (s) => ETIQUETA_ROL[s.rol] ?? s.rol, ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    {
      clave: 'ultimoLoginEn',
      titulo: 'Último ingreso',
      valor: (s) => fechaHora(s.ultimoLoginEn),
      ordenable: true,
    },
  ];

  protected readonly acciones = (fila: Miembro): AccionFila<Miembro>[] => {
    switch (fila.estado) {
      case 'ACTIVO':
        return [
          { etiqueta: 'Editar', alEjecutar: (s) => this.editar(s) },
          { etiqueta: 'Desactivar', alEjecutar: (s) => this.desactivar(s) },
        ];
      case 'INVITACION_PENDIENTE':
        return [{ etiqueta: 'Reenviar invitación', alEjecutar: (s) => this.reenviar(s) }];
      default:
        return [{ etiqueta: 'Activar', alEjecutar: (s) => this.activar(s) }];
    }
  };

  protected readonly datos = signal<Pagina<Miembro> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly consulta = new BehaviorSubject<ConsultaPagina>({ page: 0, size: 20 });
  private readonly recarga = new BehaviorSubject<void>(undefined);

  constructor() {
    // Los filtros viven en la URL; cada cambio de filtro, de página o de orden, y cada recarga piden la lista otra vez.
    combineLatest([this.ruta.queryParamMap, this.consulta, this.recarga])
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.error.set(false);
        }),
        switchMap(([filtros, consulta]) =>
          this.api
            .listarStaff(
              filtros.get('q') ?? undefined,
              (filtros.get('rol') as authApi.RolStaff | null) ?? undefined,
              (filtros.get('estado') as authApi.EstadoStaff | null) ?? undefined,
              consulta.page,
              consulta.size,
              consulta.sort,
              'body',
              false,
              { context: new HttpContext().set(SIN_TOAST, true) },
            )
            .pipe(
              tap({
                next: (respuesta) => {
                  this.datos.set((respuesta.data as Pagina<Miembro> | undefined) ?? null);
                  this.cargando.set(false);
                },
                error: () => {
                  this.cargando.set(false);
                  this.error.set(true);
                },
              }),
              catchError(() => EMPTY),
            ),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe({ error: () => undefined });
  }

  protected recargar(): void {
    this.recarga.next();
  }

  protected variante(s: Miembro): string {
    return VARIANTE_ESTADO[s.estado] ?? 'info';
  }

  protected etiquetaEstado(s: Miembro): string {
    return ETIQUETA_ESTADO_STAFF[s.estado] ?? s.estado;
  }

  protected invitar(): void {
    this.abrirDialogo({}, 'Invitación enviada');
  }

  protected editar(staff: Miembro): void {
    this.abrirDialogo({ staff }, 'Guardado');
  }

  private abrirDialogo(datos: DatosDialogoStaff, aviso: string): void {
    this.dialogos
      .open<DialogoStaff, DatosDialogoStaff, authApi.Staff | undefined>(DialogoStaff, {
        data: datos,
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (!guardado) return;
        this.toasts.mostrar('exito', aviso);
        this.recargar();
      });
  }

  protected desactivar(staff: Miembro): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Desactivar a ${staff.nombres} ${staff.apellidos}?`,
        mensaje: 'No podrá ingresar ni refrescar su sesión. Puedes reactivarlo cuando quieras.',
        accion: 'Desactivar',
        peligro: true,
      })
      .subscribe((confirmado) => {
        if (confirmado) this.cambiar(this.api.desactivarStaff(staff.id), 'Miembro desactivado');
      });
  }

  protected activar(staff: Miembro): void {
    this.cambiar(this.api.activarStaff(staff.id), 'Miembro activado');
  }

  protected reenviar(staff: Miembro): void {
    this.api.reenviarInvitacionStaff(staff.id).subscribe({
      next: () => this.toasts.mostrar('exito', 'Invitación reenviada'),
      error: () => undefined, // el interceptor ya mostró el motivo
    });
  }

  /** Aplica el cambio; si la API lo rechaza (409 con su motivo) el interceptor lo muestra y nada cambia. */
  private cambiar(llamada: Observable<unknown>, aviso: string): void {
    llamada.subscribe({
      next: () => {
        this.toasts.mostrar('exito', aviso);
        this.recargar();
      },
      error: () => undefined,
    });
  }
}
