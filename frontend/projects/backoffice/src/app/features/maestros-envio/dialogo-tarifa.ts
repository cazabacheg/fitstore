import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SIN_TOAST, camposConError, comoApiError, deliveryApi } from 'shared';
import { TarifasService } from 'shared/api/delivery-service';
import { CampoFormulario } from '../acceso/campo-formulario';
import { SelectorUbigeo, type HastaUbigeo } from '../../ui/selector-ubigeo/selector-ubigeo';

export interface DatosDialogoTarifa {
  /** Tarifa que se edita; sin ella, se crea una nueva. */
  tarifa?: deliveryApi.Tarifa;
  /** Modalidad de la pestaña desde la que se abre. */
  modalidad: deliveryApi.ModalidadEntrega;
}

const NIVELES: { valor: HastaUbigeo; etiqueta: string }[] = [
  { valor: 'distrito', etiqueta: 'Un distrito' },
  { valor: 'provincia', etiqueta: 'Toda una provincia' },
  { valor: 'departamento', etiqueta: 'Todo un departamento' },
];

const NIVEL_DE_TARIFA: Record<string, HastaUbigeo> = {
  DISTRITO: 'distrito',
  PROVINCIA: 'provincia',
  DEPARTAMENTO: 'departamento',
};

/**
 * Crear o editar una tarifa (P-6-05): modalidad, destino con el selector de ubigeo en el nivel elegido (E2: un
 * departamento, una provincia o un distrito), costo y plazo (CA-6-07 a CA-6-09).
 */
@Component({
  selector: 'fs-dialogo-tarifa',
  imports: [MatDialogModule, CampoFormulario, SelectorUbigeo],
  template: `
    <h2 mat-dialog-title>{{ tarifa ? 'Editar tarifa' : 'Nueva tarifa' }}</h2>
    <mat-dialog-content>
      <form
        id="form-tarifa"
        novalidate
        (submit)="guardar($event)"
        style="display: grid; gap: var(--espacio-4); padding-top: var(--espacio-2)"
      >
        @if (errorGeneral()) {
          <div class="fs-aviso fs-aviso--error" role="alert">
            <span class="fs-aviso__texto">{{ errorGeneral() }}</span>
          </div>
        }
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="modalidad-tarifa">Modalidad</label>
          <select
            id="modalidad-tarifa"
            class="fs-select"
            [disabled]="!!tarifa"
            (change)="cambiarModalidad(elegido($event))"
          >
            <option value="DELIVERY_LOCAL" [selected]="modalidad() === 'DELIVERY_LOCAL'">
              Delivery local
            </option>
            <option value="AGENCIA" [selected]="modalidad() === 'AGENCIA'">Agencia</option>
          </select>
          @if (modalidad() === 'DELIVERY_LOCAL') {
            <span class="fs-campo__ayuda">Solo Lima Metropolitana y Callao.</span>
          }
        </div>
        <div class="fs-campo">
          <label class="fs-campo__etiqueta" for="nivel-tarifa">Aplicar a</label>
          <select id="nivel-tarifa" class="fs-select" (change)="cambiarNivel(elegido($event))">
            @for (n of niveles; track n.valor) {
              <option [value]="n.valor" [selected]="n.valor === nivel()">{{ n.etiqueta }}</option>
            }
          </select>
        </div>
        <div class="fs-campo" [class.fs-campo--error]="!!errores().ubigeo">
          <fs-selector-ubigeo [hasta]="nivel()" [(valor)]="ubigeo" />
          @if (errores().ubigeo) {
            <span class="fs-campo__error">{{ errores().ubigeo }}</span>
          }
        </div>
        <fs-campo-formulario
          etiqueta="Costo (S/)"
          tipo="number"
          [(valor)]="costo"
          [error]="errores().costo"
        />
        <fs-campo-formulario
          etiqueta="Plazo mínimo (días)"
          tipo="number"
          [(valor)]="plazoMin"
          [error]="errores().plazoDiasMin"
        />
        <fs-campo-formulario
          etiqueta="Plazo máximo (días)"
          tipo="number"
          [(valor)]="plazoMax"
          [error]="errores().plazoDiasMax"
        />
        @if (tarifa) {
          <label class="fs-check">
            <input
              type="checkbox"
              [checked]="activa()"
              (change)="activa.set($any($event.target).checked)"
            />
            Activa
          </label>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button type="button" class="fs-btn fs-btn--contorno" [mat-dialog-close]="undefined">
        Cancelar
      </button>
      <button
        type="submit"
        form="form-tarifa"
        class="fs-btn fs-btn--primario"
        [disabled]="enviando()"
      >
        Guardar
      </button>
    </mat-dialog-actions>
  `,
})
export class DialogoTarifa {
  private readonly api = inject(TarifasService);
  private readonly ref =
    inject<MatDialogRef<DialogoTarifa, deliveryApi.Tarifa | undefined>>(MatDialogRef);
  private readonly datos = inject<DatosDialogoTarifa>(MAT_DIALOG_DATA);
  protected readonly tarifa = this.datos.tarifa;
  protected readonly niveles = NIVELES;

  protected readonly modalidad = signal<deliveryApi.ModalidadEntrega>(
    this.tarifa?.modalidad ?? this.datos.modalidad,
  );
  protected readonly nivel = signal<HastaUbigeo>(
    this.tarifa ? NIVEL_DE_TARIFA[this.tarifa.nivel] : 'distrito',
  );
  protected readonly ubigeo = signal<string | null>(this.tarifa?.ubigeo ?? null);
  protected readonly costo = signal(this.tarifa ? String(this.tarifa.costo) : '');
  protected readonly plazoMin = signal(this.tarifa ? String(this.tarifa.plazoDiasMin) : '');
  protected readonly plazoMax = signal(this.tarifa ? String(this.tarifa.plazoDiasMax) : '');
  protected readonly activa = signal(this.tarifa?.activa ?? true);
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  private readonly intentado = signal(false);
  private readonly delServidor = signal<Record<string, string>>({});

  protected readonly errores = computed(() => {
    const servidor = this.delServidor();
    const costo = Number(this.costo());
    const min = Number(this.plazoMin());
    const max = Number(this.plazoMax());
    const cliente = this.intentado()
      ? {
          ubigeo: this.ubigeo() ? '' : 'Elige el destino',
          costo: this.costo() === '' || !(costo >= 0) ? 'Ingresa un costo de 0 o más' : '',
          plazoDiasMin:
            this.plazoMin() === '' || !(min >= 0) ? 'Ingresa el plazo mínimo en días' : '',
          plazoDiasMax:
            this.plazoMax() === '' || !(max >= 0)
              ? 'Ingresa el plazo máximo en días'
              : min > max
                ? 'El plazo máximo no puede ser menor que el mínimo'
                : '',
        }
      : { ubigeo: '', costo: '', plazoDiasMin: '', plazoDiasMax: '' };
    return Object.fromEntries(
      Object.entries(cliente).map(([campo, mensaje]) => [campo, mensaje || servidor[campo] || '']),
    ) as typeof cliente;
  });

  protected elegido(evento: Event): string {
    return (evento.target as HTMLSelectElement).value;
  }

  protected cambiarModalidad(valor: string): void {
    this.modalidad.set(valor as deliveryApi.ModalidadEntrega);
  }

  protected cambiarNivel(valor: string): void {
    this.nivel.set(valor as HastaUbigeo);
    this.ubigeo.set(null);
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.intentado.set(true);
    this.errorGeneral.set('');
    this.delServidor.set({});
    const ubigeo = this.ubigeo();
    if (Object.values(this.errores()).some(Boolean) || !ubigeo || this.enviando()) return;

    const pedido: deliveryApi.TarifaRequest = {
      modalidad: this.modalidad(),
      ubigeo,
      costo: Number(this.costo()),
      plazoDiasMin: Number(this.plazoMin()),
      plazoDiasMax: Number(this.plazoMax()),
      activa: this.tarifa ? this.activa() : true,
    };
    const contexto = { context: new HttpContext().set(SIN_TOAST, true) };
    const llamada = this.tarifa
      ? this.api.editarTarifa(this.tarifa.id, pedido, 'body', false, contexto)
      : this.api.crearTarifa(pedido, 'body', false, contexto);
    this.enviando.set(true);
    llamada.subscribe({
      next: (r) => {
        this.enviando.set(false);
        if (r.data) this.ref.close(r.data);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        const cuerpo = comoApiError(error);
        const campos = camposConError(error);
        if (campos.length > 0) {
          this.delServidor.set(Object.fromEntries(campos.map((c) => [c.campo, c.mensaje])));
        } else if (cuerpo?.codigo === 'TARIFA_DUPLICADA') {
          this.delServidor.set({ ubigeo: cuerpo.mensaje });
        } else {
          this.errorGeneral.set(cuerpo?.mensaje ?? 'No pudimos guardar. Inténtalo en un momento.');
        }
      },
    });
  }
}
