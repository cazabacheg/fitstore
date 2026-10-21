import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  model,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  MatAutocompleteModule,
  type MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { type OperatorFunction, Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import { UbigeoCatalogo, departamentoDe, provinciaDe, settingsApi } from 'shared';

/** Espera del buscador antes de consultar: solo se pide el texto final, no cada tecla. */
const ESPERA_BUSCADOR_MS = 300;

/** Hasta qué nivel se elige: el código emitido tiene 2, 4 o 6 dígitos (CA-0b-26). */
export type HastaUbigeo = 'departamento' | 'provincia' | 'distrito';

/** Si /ubigeo falla, la lista queda vacía: el selector sigue usable y no deja un error sin manejar. */
function sinError<T>(): OperatorFunction<T[], T[]> {
  return catchError(() => of([] as T[]));
}

/**
 * Selector de ubigeo en tres niveles (departamento › provincia › distrito) con buscador de distritos.
 * Emite el código INEI por `valor` solo cuando llega al nivel `hasta`; mientras tanto el valor es `null`.
 * Las listas vienen de `/ubigeo` con la caché del navegador de `UbigeoCatalogo`.
 */
@Component({
  selector: 'fs-selector-ubigeo',
  imports: [MatFormFieldModule, MatInputModule, MatAutocompleteModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './selector-ubigeo.html',
  styleUrl: './selector-ubigeo.css',
})
export class SelectorUbigeo {
  private readonly catalogo = inject(UbigeoCatalogo);
  private readonly destruir = inject(DestroyRef);

  /** Nivel en el que se detiene (por defecto, el distrito). */
  readonly hasta = input<HastaUbigeo>('distrito');
  /** Código elegido: 2, 4 o 6 dígitos según `hasta`; `null` mientras falta algún nivel. */
  readonly valor = model<string | null>(null);

  protected readonly departamento = signal('');
  protected readonly provincia = signal('');
  protected readonly distrito = signal('');
  protected readonly departamentos = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly provincias = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly distritos = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly resultados = signal<settingsApi.DistritoEncontrado[]>([]);
  protected readonly niveles = computed(
    () => ({ departamento: 1, provincia: 2, distrito: 3 })[this.hasta()],
  );

  private ultimoCodigo: string | null = null;
  private readonly busqueda = new Subject<string>();

  constructor() {
    this.catalogo
      .departamentos()
      .pipe(sinError())
      .subscribe((lista) => this.departamentos.set(lista));
    this.busqueda
      .pipe(
        debounceTime(ESPERA_BUSCADOR_MS),
        switchMap((texto) =>
          texto.trim().length < 2
            ? of([])
            : this.catalogo.buscarDistritos(texto).pipe(catchError(() => of([]))),
        ),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe((lista) => this.resultados.set(lista));
    // Un código que llega de fuera (edición de una dirección) llena los selects.
    effect(() => {
      const codigo = this.valor();
      if (codigo !== this.ultimoCodigo) {
        this.ultimoCodigo = codigo;
        untracked(() => this.llenarDesde(codigo));
      }
    });
  }

  protected elegirDepartamento(codigo: string): void {
    this.departamento.set(codigo);
    this.provincia.set('');
    this.distrito.set('');
    this.provincias.set([]);
    this.distritos.set([]);
    if (codigo)
      this.catalogo
        .provincias(codigo)
        .pipe(sinError())
        .subscribe((lista) => this.provincias.set(lista));
    this.publicar();
  }

  protected elegirProvincia(codigo: string): void {
    this.provincia.set(codigo);
    this.distrito.set('');
    this.distritos.set([]);
    if (codigo)
      this.catalogo
        .distritos(codigo)
        .pipe(sinError())
        .subscribe((lista) => this.distritos.set(lista));
    this.publicar();
  }

  protected elegirDistrito(codigo: string): void {
    this.distrito.set(codigo);
    this.publicar();
  }

  protected buscar(texto: string): void {
    if (texto.trim().length < 2) this.resultados.set([]);
    this.busqueda.next(texto);
  }

  protected elegirResultado(
    evento: MatAutocompleteSelectedEvent,
    buscador: HTMLInputElement,
  ): void {
    const encontrado = evento.option.value as settingsApi.DistritoEncontrado;
    this.llenarDesde(encontrado.codigo);
    this.publicar();
    buscador.value = '';
    this.busqueda.next('');
    this.resultados.set([]);
  }

  protected etiquetaResultado(r: settingsApi.DistritoEncontrado): string {
    return `${r.nombre} · ${r.provincia}, ${r.departamento}`;
  }

  private llenarDesde(codigo: string | null): void {
    this.departamento.set(codigo ? departamentoDe(codigo) : '');
    this.provincia.set(codigo && codigo.length >= 4 ? provinciaDe(codigo) : '');
    this.distrito.set(codigo?.length === 6 ? codigo : '');
    this.provincias.set([]);
    this.distritos.set([]);
    if (this.departamento())
      this.catalogo
        .provincias(this.departamento())
        .pipe(sinError())
        .subscribe((l) => this.provincias.set(l));
    if (this.provincia())
      this.catalogo
        .distritos(this.provincia())
        .pipe(sinError())
        .subscribe((l) => this.distritos.set(l));
  }

  private publicar(): void {
    const codigo =
      { departamento: this.departamento(), provincia: this.provincia(), distrito: this.distrito() }[
        this.hasta()
      ] || null;
    if (codigo !== this.ultimoCodigo) {
      this.ultimoCodigo = codigo;
      this.valor.set(codigo);
    }
  }
}
