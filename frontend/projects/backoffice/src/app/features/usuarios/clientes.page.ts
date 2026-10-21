import { HttpContext } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, EMPTY, catchError, combineLatest, switchMap, tap } from 'rxjs';
import { SIN_TOAST, authApi, type ConsultaPagina, type Pagina } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { EstadoError } from '../../ui/estados/estado-error';
import { BarraFiltros, type SelectFiltro } from '../../ui/filtros/barra-filtros';
import { FsCelda } from '../../ui/tabla/fs-celda';
import { FsTabla, type ColumnaTabla } from '../../ui/tabla/fs-tabla';

type Fila = authApi.ClienteResumen;

/** Clientes (P-1-09): búsqueda por nombre, correo o DNI. El nombre abre `/clientes/:id`; la fila lleva "Ver ›" (R11). */
@Component({
  selector: 'fs-clientes-page',
  imports: [EncabezadoPagina, BarraFiltros, FsTabla, FsCelda, EstadoError],
  template: `
    <fs-encabezado-pagina titulo="Clientes" />
    <fs-barra-filtros placeholder="Buscar por nombre, correo o DNI" [selects]="selects" />
    @if (error()) {
      <fs-estado-error titulo="No pudimos cargar los clientes" (reintentar)="recargar()" />
    } @else {
      <fs-tabla
        [columnas]="columnas"
        [datos]="datos()"
        [cargando]="cargando()"
        columnaId="nombres"
        [filaEnlace]="enlace"
        tituloVacio="Sin resultados"
        textoVacio="Prueba con otro nombre, correo o DNI."
        (consulta)="consulta.next($event)"
      >
        <ng-template fsCelda="activo" let-fila>
          <span
            class="fs-insignia"
            [class]="'fs-insignia fs-insignia--' + (fila.activo ? 'exito' : 'error')"
            >{{ fila.activo ? 'Activo' : 'Bloqueado' }}</span
          >
        </ng-template>
      </fs-tabla>
    }
  `,
})
export class ClientesPage {
  private readonly api = inject(authApi.ClientesAdminService);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly selects: SelectFiltro[] = [
    {
      clave: 'activo',
      etiqueta: 'Estado',
      opciones: [
        { valor: 'true', etiqueta: 'Activo' },
        { valor: 'false', etiqueta: 'Bloqueado' },
      ],
    },
  ];

  protected readonly columnas: ColumnaTabla<Fila>[] = [
    {
      clave: 'nombres',
      titulo: 'Nombre',
      valor: (c) => `${c.nombres} ${c.apellidos}`,
      ordenable: true,
    },
    { clave: 'email', titulo: 'Correo', valor: (c) => c.email, ordenable: true },
    { clave: 'dni', titulo: 'DNI', valor: (c) => c.dni, ordenable: true },
    { clave: 'telefono', titulo: 'Celular', valor: (c) => c.telefono },
    { clave: 'activo', titulo: 'Estado', ordenable: true },
  ];

  protected readonly enlace = (c: Fila) => ['/clientes', c.id];

  protected readonly datos = signal<Pagina<Fila> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly consulta = new BehaviorSubject<ConsultaPagina>({ page: 0, size: 20 });
  private readonly recarga = new BehaviorSubject<void>(undefined);

  constructor() {
    combineLatest([this.ruta.queryParamMap, this.consulta, this.recarga])
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.error.set(false);
        }),
        switchMap(([filtros, consulta]) => {
          const activo = filtros.get('activo');
          return this.api
            .listarClientes(
              filtros.get('q') ?? undefined,
              activo === null ? undefined : activo === 'true',
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
                  this.datos.set((respuesta.data as Pagina<Fila> | undefined) ?? null);
                  this.cargando.set(false);
                },
                error: () => {
                  this.cargando.set(false);
                  this.error.set(true);
                },
              }),
              catchError(() => EMPTY),
            );
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe({ error: () => undefined });
  }

  protected recargar(): void {
    this.recarga.next();
  }
}
