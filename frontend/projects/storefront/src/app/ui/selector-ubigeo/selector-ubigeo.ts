import { Component, effect, inject, input, output, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import { UbigeoCatalogo, departamentoDe, provinciaDe, type NivelUbigeo } from 'shared';
import type { settingsApi } from 'shared';
import { idUnico } from '../id-unico';

/** Espera del buscador antes de consultar (la misma que el selector del backoffice). */
const ESPERA_BUSCADOR_MS = 300;

/**
 * Selector de ubigeo en tres niveles con buscador de distritos; emite el código INEI (sistema de diseño:
 * SelectorUbigeo, CA-0b-26). Con `hasta` = "departamento" o "provincia" se detiene en ese nivel.
 * Las listas vienen de `/ubigeo` con caché en el navegador (`UbigeoCatalogo`).
 */
@Component({
  selector: 'fs-selector-ubigeo',
  template: `
    @if (hasta() === 'distrito') {
      <div class="fs-campo" style="margin-bottom: var(--espacio-3)">
        <label class="fs-campo__etiqueta" [for]="id + '-buscar'">Buscar distrito</label>
        <input
          class="fs-input"
          type="search"
          autocomplete="off"
          [id]="id + '-buscar'"
          [value]="texto()"
          (input)="buscar($event)"
        />
        @if (resultados().length) {
          <ul class="fs-ubigeo-resultados" role="listbox" aria-label="Distritos encontrados">
            @for (r of resultados(); track r.codigo) {
              <li role="presentation">
                <button
                  type="button"
                  role="option"
                  aria-selected="false"
                  (click)="elegirResultado(r)"
                >
                  {{ r.nombre }}, {{ r.provincia }}, {{ r.departamento }}
                </button>
              </li>
            }
          </ul>
        }
      </div>
    }
    <div class="fs-ubigeo" [style.grid-template-columns]="columnas()">
      <div class="fs-campo">
        <label class="fs-campo__etiqueta" [for]="id + '-dep'">Departamento</label>
        <select class="fs-select" [id]="id + '-dep'" (change)="elegirDepartamento($event)">
          <option value="" [selected]="!dep()">Elige un departamento</option>
          @for (o of departamentos(); track o.codigo) {
            <option [value]="o.codigo" [selected]="o.codigo === dep()">{{ o.nombre }}</option>
          }
        </select>
      </div>
      @if (hasta() !== 'departamento') {
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" [for]="id + '-prov'">Provincia</label>
          <select
            class="fs-select"
            [id]="id + '-prov'"
            [disabled]="!dep()"
            (change)="elegirProvincia($event)"
          >
            <option value="" [selected]="!prov()">Elige una provincia</option>
            @for (o of provincias(); track o.codigo) {
              <option [value]="o.codigo" [selected]="o.codigo === prov()">{{ o.nombre }}</option>
            }
          </select>
        </div>
      }
      @if (hasta() === 'distrito') {
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" [for]="id + '-dist'">Distrito</label>
          <select
            class="fs-select"
            [id]="id + '-dist'"
            [disabled]="!prov()"
            (change)="elegirDistrito($event)"
          >
            <option value="" [selected]="!dist()">Elige un distrito</option>
            @for (o of distritos(); track o.codigo) {
              <option [value]="o.codigo" [selected]="o.codigo === dist()">{{ o.nombre }}</option>
            }
          </select>
        </div>
      }
    </div>
  `,
})
export class FsSelectorUbigeo {
  private readonly catalogo = inject(UbigeoCatalogo);
  protected readonly id = idUnico('fs-ubigeo');

  /** Último nivel que se pide (por defecto, el distrito). */
  readonly hasta = input<NivelUbigeo>('distrito');
  /** Código inicial (2, 4 o 6 dígitos). */
  readonly valor = input<string | null>(null);
  /** Código elegido, o `null` si la selección quedó incompleta. */
  readonly cambio = output<string | null>();

  protected readonly departamentos = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly provincias = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly distritos = signal<settingsApi.UbigeoItem[]>([]);
  protected readonly dep = signal('');
  protected readonly prov = signal('');
  protected readonly dist = signal('');
  protected readonly texto = signal('');
  protected readonly resultados = signal<settingsApi.DistritoEncontrado[]>([]);

  /** Textos del buscador: se consulta 300 ms después de la última tecla, y solo el texto final. */
  private readonly busqueda = new Subject<string>();

  constructor() {
    this.catalogo.departamentos().subscribe((lista) => this.departamentos.set(lista));
    this.busqueda
      .pipe(
        debounceTime(ESPERA_BUSCADOR_MS),
        switchMap((texto) =>
          texto.trim().length < 2
            ? of([])
            : this.catalogo.buscarDistritos(texto).pipe(catchError(() => of([]))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((lista) => this.resultados.set(lista));
    effect(() => {
      const inicial = this.valor();
      untracked(() => this.aplicarInicial(inicial));
    });
  }

  protected columnas(): string {
    const n = this.hasta() === 'departamento' ? 1 : this.hasta() === 'provincia' ? 2 : 3;
    return `repeat(${n}, minmax(0, 1fr))`;
  }

  protected elegirDepartamento(evento: Event): void {
    const codigo = (evento.target as HTMLSelectElement).value;
    this.dep.set(codigo);
    this.prov.set('');
    this.dist.set('');
    this.provincias.set([]);
    this.distritos.set([]);
    if (codigo && this.hasta() !== 'departamento') this.cargarProvincias(codigo);
    this.cambio.emit(codigo && this.hasta() === 'departamento' ? codigo : null);
  }

  protected elegirProvincia(evento: Event): void {
    const codigo = (evento.target as HTMLSelectElement).value;
    this.prov.set(codigo);
    this.dist.set('');
    this.distritos.set([]);
    if (codigo && this.hasta() === 'distrito') this.cargarDistritos(codigo);
    this.cambio.emit(codigo && this.hasta() === 'provincia' ? codigo : null);
  }

  protected elegirDistrito(evento: Event): void {
    const codigo = (evento.target as HTMLSelectElement).value;
    this.dist.set(codigo);
    this.cambio.emit(codigo || null);
  }

  protected buscar(evento: Event): void {
    const texto = (evento.target as HTMLInputElement).value;
    this.texto.set(texto);
    if (texto.trim().length < 2) this.resultados.set([]);
    this.busqueda.next(texto);
  }

  protected elegirResultado(r: settingsApi.DistritoEncontrado): void {
    this.busqueda.next('');
    this.texto.set('');
    this.resultados.set([]);
    this.aplicarInicial(r.codigo);
    this.cambio.emit(r.codigo);
  }

  private aplicarInicial(codigo: string | null): void {
    this.dep.set(codigo ? departamentoDe(codigo) : '');
    this.prov.set(codigo && codigo.length >= 4 ? provinciaDe(codigo) : '');
    this.dist.set(codigo && codigo.length === 6 ? codigo : '');
    this.provincias.set([]);
    this.distritos.set([]);
    if (this.dep() && this.hasta() !== 'departamento') this.cargarProvincias(this.dep());
    if (this.prov() && this.hasta() === 'distrito') this.cargarDistritos(this.prov());
  }

  private cargarProvincias(departamento: string): void {
    this.catalogo.provincias(departamento).subscribe((l) => this.provincias.set(l));
  }

  private cargarDistritos(provincia: string): void {
    this.catalogo.distritos(provincia).subscribe((l) => this.distritos.set(l));
  }
}
