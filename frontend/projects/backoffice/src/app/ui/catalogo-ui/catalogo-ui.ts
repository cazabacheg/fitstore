import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { EstadoServicio, ToastService, type Pagina } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../dialogo/dialogo.service';
import { EstadoCargando } from '../estados/estado-cargando';
import { EstadoError } from '../estados/estado-error';
import { EstadoVacio } from '../estados/estado-vacio';
import { BarraFiltros, type ChipFiltro, type SelectFiltro } from '../filtros/barra-filtros';
import { ESTADOS, type TipoEstado } from '../insignia/estados';
import { InsigniaEstado } from '../insignia/insignia-estado';
import { SelectorUbigeo } from '../selector-ubigeo/selector-ubigeo';
import { SubidaImagenes } from '../subida-imagenes/subida-imagenes';
import { FsCelda } from '../tabla/fs-celda';
import { FsTabla, type AccionFila, type ColumnaTabla } from '../tabla/fs-tabla';

interface PedidoEjemplo {
  codigo: string;
  cliente: string;
  estado: string;
  total: number;
  porVencer: boolean;
}

const FILAS: PedidoEjemplo[] = [
  { codigo: 'FS-000119', cliente: 'Luis Rojas', estado: 'PAGADO', total: 129.8, porVencer: false },
  {
    codigo: 'FS-000121',
    cliente: 'Ana Quispe',
    estado: 'PENDIENTE_PAGO',
    total: 59.9,
    porVencer: true,
  },
  { codigo: 'FS-000123', cliente: 'Rosa Díaz', estado: 'ENVIADO', total: 184.7, porVencer: false },
];

/** Catálogo de componentes de `ui/` del backoffice, con sus variantes y estados (P-0b-04). Solo existe en desarrollo (CA-0b-25). */
@Component({
  selector: 'fs-catalogo-ui',
  imports: [
    MatButtonModule,
    EncabezadoPagina,
    EstadoCargando,
    EstadoVacio,
    EstadoError,
    FsTabla,
    FsCelda,
    BarraFiltros,
    InsigniaEstado,
    SelectorUbigeo,
    SubidaImagenes,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './catalogo-ui.html',
  styleUrl: './catalogo-ui.css',
})
export class CatalogoUi {
  private readonly toasts = inject(ToastService);
  private readonly dialogo = inject(DialogoService);
  protected readonly servicio = inject(EstadoServicio);

  protected readonly tipos = Object.keys(ESTADOS) as TipoEstado[];
  protected readonly codigos = (tipo: TipoEstado): string[] => Object.keys(ESTADOS[tipo]);

  protected readonly columnas: ColumnaTabla<PedidoEjemplo>[] = [
    { clave: 'codigo', titulo: 'Pedido', valor: (f) => f.codigo, ordenable: true },
    { clave: 'cliente', titulo: 'Cliente', valor: (f) => f.cliente },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'total', titulo: 'Total', valor: (f) => `S/ ${f.total.toFixed(2)}`, ordenable: true },
  ];
  protected readonly pagina: Pagina<PedidoEjemplo> = {
    contenido: FILAS,
    pagina: 0,
    tamano: 20,
    totalElementos: 64,
    totalPaginas: 4,
  };
  protected readonly vacia: Pagina<PedidoEjemplo> = {
    contenido: [],
    pagina: 0,
    tamano: 20,
    totalElementos: 0,
    totalPaginas: 0,
  };
  protected readonly enlace = (): string[] => ['/_ui'];
  protected readonly clase = (f: PedidoEjemplo) => (f.porVencer ? ('aviso' as const) : null);
  protected readonly acciones = (): AccionFila<PedidoEjemplo>[] => [
    { etiqueta: 'Asignar', alEjecutar: () => this.toasts.mostrar('info', 'Asignar') },
    { etiqueta: 'Cancelar', alEjecutar: () => this.toasts.mostrar('info', 'Cancelar') },
    { etiqueta: 'Reembolsar', alEjecutar: () => this.toasts.mostrar('info', 'Reembolsar') },
  ];

  protected readonly chips: ChipFiltro[] = [
    { valor: 'PENDIENTE_PAGO', etiqueta: 'Pendiente de pago' },
    { valor: 'PAGADO', etiqueta: 'Pagado' },
    { valor: 'ENVIADO', etiqueta: 'Enviado' },
  ];
  protected readonly contadores = { PENDIENTE_PAGO: 4, PAGADO: 7, ENVIADO: 2 };
  protected readonly selects: SelectFiltro[] = [
    {
      clave: 'modalidad',
      etiqueta: 'Modalidad',
      opciones: [
        { valor: 'RECOJO', etiqueta: 'Recojo' },
        { valor: 'DELIVERY', etiqueta: 'Delivery local' },
      ],
    },
  ];
  protected readonly ubigeo = signal<string | null>(null);

  protected avisar(tipo: 'exito' | 'error' | 'aviso' | 'info', mensaje: string): void {
    this.toasts.mostrar(tipo, mensaje);
  }

  protected confirmar(peligro: boolean): void {
    this.dialogo
      .confirmar({
        titulo: peligro ? '¿Cancelar el pedido FS-000124?' : '¿Confirmar el pedido FS-000124?',
        mensaje: peligro
          ? 'El stock reservado se libera.'
          : 'Se enviará el correo de confirmación.',
        accion: peligro ? 'Cancelar pedido' : 'Confirmar pedido',
        peligro,
      })
      .subscribe((si) => this.toasts.mostrar('info', si ? 'Confirmado' : 'Sin cambios'));
  }
}
