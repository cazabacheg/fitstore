import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, debounceTime, map } from 'rxjs';

export interface ChipFiltro {
  valor: string;
  etiqueta: string;
}
export interface SelectFiltro {
  clave: string;
  etiqueta: string;
  opciones: ChipFiltro[];
}

/** Espera del buscador antes de consultar (ficha `BarraFiltros`). */
const ESPERA_BUSCADOR_MS = 400;
/** Controles visibles (el buscador cuenta); el resto va en "Más filtros". */
const CONTROLES_VISIBLES = 4;

/**
 * Barra de filtros de los listados: buscador, selects y chips de estado con contador (K6).
 * Los filtros viven en la URL (`?estado=PAGADO&q=ana`) para que el enlace se comparta y "Atrás" funcione;
 * `cambio` emite todos los filtros activos cada vez que la persona cambia uno.
 */
@Component({
  selector: 'fs-barra-filtros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './barra-filtros.html',
})
export class BarraFiltros {
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  readonly placeholder = input('Buscar');
  readonly selects = input<SelectFiltro[]>([]);
  readonly chips = input<ChipFiltro[]>([]);
  /** Conteo por estado del contrato; los chips no aplican el filtro de estado a sus propios números. */
  readonly contadores = input<Record<string, number> | null>(null);
  /** Parámetro de la URL del chip activo. */
  readonly claveChip = input('estado');

  readonly cambio = output<Record<string, string>>();

  private readonly parametros = toSignal(this.ruta.queryParamMap.pipe(map((m) => m)), {
    initialValue: this.ruta.snapshot.queryParamMap,
  });
  protected readonly valor = (clave: string): string => this.parametros().get(clave) ?? '';
  protected readonly masAbierto = signal(false);
  private readonly busqueda = new Subject<string>();

  protected readonly selectsVisibles = computed(() =>
    this.selects().slice(0, CONTROLES_VISIBLES - 1),
  );
  protected readonly selectsExtra = computed(() => this.selects().slice(CONTROLES_VISIBLES - 1));
  protected readonly total = computed(() => {
    const c = this.contadores();
    return c ? Object.values(c).reduce((suma, n) => suma + n, 0) : null;
  });
  protected readonly hayActivos = computed(() => Object.keys(this.activos()).length > 0);

  constructor() {
    this.busqueda
      .pipe(debounceTime(ESPERA_BUSCADOR_MS), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((texto) => this.poner('q', texto.trim()));
  }

  protected escribir(texto: string): void {
    this.busqueda.next(texto);
  }

  protected poner(clave: string, valor: string): void {
    this.aplicar({ ...this.activos(), [clave]: valor });
  }

  protected limpiar(): void {
    this.aplicar({});
  }

  /** Filtros activos (sin vacíos), tomados de la URL. */
  private activos(): Record<string, string> {
    const claves = ['q', this.claveChip(), ...this.selects().map((s) => s.clave)];
    const resultado: Record<string, string> = {};
    for (const clave of claves) {
      const v = this.valor(clave);
      if (v) resultado[clave] = v;
    }
    return resultado;
  }

  private aplicar(filtros: Record<string, string>): void {
    const limpios = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v !== ''));
    const params: Record<string, string | null> = {};
    for (const clave of ['q', this.claveChip(), ...this.selects().map((s) => s.clave)])
      params[clave] = limpios[clave] ?? null;
    this.cambio.emit(limpios);
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }
}
