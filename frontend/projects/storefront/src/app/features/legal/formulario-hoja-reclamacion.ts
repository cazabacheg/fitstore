import { HttpContext, HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  output,
  signal,
} from '@angular/core';
import { SIN_TOAST, camposConError, settingsApi } from 'shared';
import { FsBoton } from '../../ui/boton/boton';
import { FsCampo } from '../../ui/campo/campo';
import { FsCheckbox } from '../../ui/checkbox/checkbox';
import { FsSelect, type OpcionSelect } from '../../ui/select/select';

/** Lo que la pantalla necesita mostrar después de registrar el reclamo. */
export interface ReclamoEnviado {
  numero: string;
  fechaLimite: string;
  email: string;
}

type Campo =
  | 'nombres'
  | 'apellidos'
  | 'numeroDocumento'
  | 'domicilio'
  | 'telefono'
  | 'email'
  | 'apoderado'
  | 'bienDescripcion'
  | 'monto'
  | 'pedidoCodigo'
  | 'detalle'
  | 'pedidoConsumidor';

/** Campo del contrato (K5) → campo del formulario, para mostrar el error del servidor donde corresponde. */
const CAMPO_DEL_SERVIDOR: Record<string, Campo> = {
  'consumidor.nombres': 'nombres',
  'consumidor.apellidos': 'apellidos',
  'consumidor.numeroDocumento': 'numeroDocumento',
  'consumidor.domicilio': 'domicilio',
  'consumidor.telefono': 'telefono',
  'consumidor.email': 'email',
  'consumidor.apoderadoNombre': 'apoderado',
  'bien.descripcion': 'bienDescripcion',
  'bien.montoReclamado': 'monto',
  'bien.pedidoCodigo': 'pedidoCodigo',
  detalle: 'detalle',
  pedidoConsumidor: 'pedidoConsumidor',
};

const DOCUMENTOS: OpcionSelect[] = [
  { valor: 'DNI', etiqueta: 'DNI' },
  { valor: 'CE', etiqueta: 'Carné de extranjería' },
];
const BIENES: OpcionSelect[] = [
  { valor: 'PRODUCTO', etiqueta: 'Producto' },
  { valor: 'SERVICIO', etiqueta: 'Servicio' },
];

/**
 * Hoja de Reclamación virtual (P-3-03). Valida en el navegador lo mismo que el servidor; los errores del servidor
 * se muestran en su campo. Si el consumidor es menor de edad se exige el nombre de su padre, madre o apoderado.
 */
@Component({
  selector: 'fs-formulario-hoja-reclamacion',
  imports: [FsCampo, FsSelect, FsCheckbox, FsBoton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="hoja" novalidate (submit)="enviar($event)">
      <fieldset class="hoja__bloque">
        <legend>Tus datos</legend>
        <div class="hoja__rejilla">
          <fs-campo etiqueta="Nombres" [(valor)]="nombres" [error]="error('nombres')" />
          <fs-campo etiqueta="Apellidos" [(valor)]="apellidos" [error]="error('apellidos')" />
          <fs-select
            etiqueta="Tipo de documento"
            [opciones]="documentos"
            [(valor)]="tipoDocumento"
          />
          <fs-campo
            etiqueta="Número de documento"
            [(valor)]="numeroDocumento"
            [error]="error('numeroDocumento')"
          />
          <fs-campo etiqueta="Domicilio" [(valor)]="domicilio" [error]="error('domicilio')" />
          <fs-campo
            etiqueta="Teléfono"
            tipo="tel"
            [(valor)]="telefono"
            [error]="error('telefono')"
          />
          <fs-campo
            etiqueta="Correo electrónico"
            tipo="email"
            [(valor)]="email"
            [error]="error('email')"
            ayuda="Te enviaremos una copia de tu reclamo y la respuesta."
          />
        </div>
        <fs-checkbox etiqueta="Soy menor de edad" [(marcado)]="esMenor" />
        @if (esMenor()) {
          <fs-campo
            etiqueta="Nombre del padre, madre o apoderado"
            [(valor)]="apoderado"
            [error]="error('apoderado')"
          />
        }
      </fieldset>

      <fieldset class="hoja__bloque">
        <legend>Bien contratado</legend>
        <div class="hoja__rejilla">
          <fs-select etiqueta="Bien contratado" [opciones]="bienes" [(valor)]="bienTipo" />
          <fs-campo
            etiqueta="Descripción del bien"
            [(valor)]="bienDescripcion"
            [error]="error('bienDescripcion')"
          />
          <fs-campo
            etiqueta="Monto reclamado (S/)"
            tipo="number"
            [(valor)]="monto"
            [error]="error('monto')"
            ayuda="Opcional."
          />
          <fs-campo
            etiqueta="Código de pedido (opcional)"
            [(valor)]="pedidoCodigo"
            [error]="error('pedidoCodigo')"
            placeholder="FS-000118"
          />
        </div>
      </fieldset>

      <fieldset class="hoja__bloque">
        <legend>Tu reclamo o queja</legend>
        <div class="hoja__tipos">
          <label class="fs-check">
            <input
              type="radio"
              name="tipo-reclamo"
              value="RECLAMO"
              [checked]="tipo() === 'RECLAMO'"
              (change)="tipo.set('RECLAMO')"
            />
            <span><strong>Reclamo:</strong> disconformidad con el producto o servicio.</span>
          </label>
          <label class="fs-check">
            <input
              type="radio"
              name="tipo-reclamo"
              value="QUEJA"
              [checked]="tipo() === 'QUEJA'"
              (change)="tipo.set('QUEJA')"
            />
            <span
              ><strong>Queja:</strong> malestar con la atención, sin relación con el producto.</span
            >
          </label>
        </div>
        <div class="fs-campo" [class.fs-campo--error]="!!error('detalle')">
          <label class="fs-campo__etiqueta" for="hoja-detalle">Detalle de lo ocurrido</label>
          <textarea
            id="hoja-detalle"
            class="fs-input"
            rows="5"
            [value]="detalle()"
            [attr.aria-invalid]="error('detalle') ? 'true' : null"
            (input)="detalle.set($any($event.target).value)"
          ></textarea>
          @if (error('detalle')) {
            <span class="fs-campo__error"
              ><span class="fs-icono" aria-hidden="true">error</span>{{ error('detalle') }}</span
            >
          }
        </div>
        <div class="fs-campo" [class.fs-campo--error]="!!error('pedidoConsumidor')">
          <label class="fs-campo__etiqueta" for="hoja-pedido"
            >Pedido del consumidor: qué solicitas</label
          >
          <textarea
            id="hoja-pedido"
            class="fs-input"
            rows="3"
            [value]="pedidoConsumidor()"
            [attr.aria-invalid]="error('pedidoConsumidor') ? 'true' : null"
            (input)="pedidoConsumidor.set($any($event.target).value)"
          ></textarea>
          @if (error('pedidoConsumidor')) {
            <span class="fs-campo__error"
              ><span class="fs-icono" aria-hidden="true">error</span
              >{{ error('pedidoConsumidor') }}</span
            >
          }
        </div>
      </fieldset>

      @if (errorEnvio(); as mensaje) {
        <p class="hoja__error" role="alert" data-testid="error-envio">{{ mensaje }}</p>
      }
      <button type="submit" fsBoton variante="primario" [disabled]="enviando()">
        {{ enviando() ? 'Enviando…' : 'Enviar hoja de reclamación' }}
      </button>
    </form>
  `,
  styles: `
    .hoja {
      display: grid;
      gap: 24px;
    }
    .hoja__bloque {
      display: grid;
      gap: 16px;
      margin: 0;
      padding: 20px;
      border: 1px solid var(--fs-color-linea);
      border-radius: var(--fs-radio-md, 8px);
    }
    .hoja__bloque legend {
      padding-inline: 8px;
      font-weight: 600;
    }
    .hoja__rejilla {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }
    .hoja__tipos {
      display: grid;
      gap: 8px;
    }
    .hoja__error {
      margin: 0;
      padding: 12px 16px;
      border-radius: var(--fs-radio-md, 8px);
      background: var(--fs-color-error-suave, #fdecea);
      color: var(--fs-color-error, #b3261e);
    }
  `,
})
export class FormularioHojaReclamacion {
  private readonly api = inject(settingsApi.ReclamosService);

  readonly enviado = output<ReclamoEnviado>();

  protected readonly documentos = DOCUMENTOS;
  protected readonly bienes = BIENES;

  protected readonly nombres = signal('');
  protected readonly apellidos = signal('');
  protected readonly tipoDocumento = signal('DNI');
  protected readonly numeroDocumento = signal('');
  protected readonly domicilio = signal('');
  protected readonly telefono = signal('');
  protected readonly email = signal('');
  protected readonly esMenor = signal(false);
  protected readonly apoderado = signal('');
  protected readonly bienTipo = signal('PRODUCTO');
  protected readonly bienDescripcion = signal('');
  protected readonly monto = signal('');
  protected readonly pedidoCodigo = signal('');
  protected readonly tipo = signal<'RECLAMO' | 'QUEJA'>('RECLAMO');
  protected readonly detalle = signal('');
  protected readonly pedidoConsumidor = signal('');

  protected readonly enviando = signal(false);
  protected readonly errorEnvio = signal('');
  private readonly intentado = signal(false);
  private readonly erroresServidor = signal<Partial<Record<Campo, string>>>({});

  /** Errores de validación del navegador, por campo. */
  protected readonly erroresLocales = computed<Partial<Record<Campo, string>>>(() => {
    const e: Partial<Record<Campo, string>> = {};
    const largo = (valor: string, min: number, max: number, nombre: string): string | undefined => {
      const v = valor.trim();
      if (v.length === 0) return `Indica ${nombre}`;
      if (v.length < min) return `Escribe al menos ${min} caracteres`;
      if (v.length > max) return `Escribe como máximo ${max} caracteres`;
      return undefined;
    };
    e.nombres = largo(this.nombres(), 2, 80, 'tus nombres');
    e.apellidos = largo(this.apellidos(), 2, 80, 'tus apellidos');
    const documento = this.numeroDocumento().trim();
    if (documento.length === 0) e.numeroDocumento = 'Indica tu número de documento';
    else if (this.tipoDocumento() === 'DNI' && !/^\d{8}$/.test(documento))
      e.numeroDocumento = 'El DNI debe tener 8 dígitos';
    else if (this.tipoDocumento() === 'CE' && !/^[0-9A-Za-z]{8,12}$/.test(documento))
      e.numeroDocumento = 'El carné de extranjería debe tener de 8 a 12 caracteres';
    e.domicilio = largo(this.domicilio(), 5, 200, 'tu domicilio');
    e.telefono = largo(this.telefono(), 6, 15, 'tu teléfono');
    const correo = this.email().trim();
    if (correo.length === 0) e.email = 'Indica tu correo';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo) || correo.length > 150)
      e.email = 'Correo inválido';
    if (this.esMenor() && this.apoderado().trim().length === 0)
      e.apoderado = 'Indica el nombre del padre, madre o apoderado';
    e.bienDescripcion = largo(this.bienDescripcion(), 3, 300, 'qué compraste');
    const monto = this.monto().trim();
    if (monto.length > 0) {
      if (!/^\d+(\.\d{1,2})?$/.test(monto)) e.monto = 'Indica un monto válido, mayor o igual a 0';
    }
    const pedido = this.pedidoCodigo().trim();
    if (pedido.length > 0 && !/^FS-\d{6}$/.test(pedido))
      e.pedidoCodigo = 'El código tiene el formato FS-000118';
    e.detalle = largo(this.detalle(), 10, 3000, 'qué ocurrió');
    e.pedidoConsumidor = largo(this.pedidoConsumidor(), 5, 1000, 'qué solicitas');
    return e;
  });

  protected error(campo: Campo): string {
    const servidor = this.erroresServidor()[campo];
    if (servidor) return servidor;
    return this.intentado() ? (this.erroresLocales()[campo] ?? '') : '';
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    if (this.enviando()) return;
    this.intentado.set(true);
    this.erroresServidor.set({});
    this.errorEnvio.set('');
    if (Object.values(this.erroresLocales()).some(Boolean)) return;

    const monto = this.monto().trim();
    const pedido = this.pedidoCodigo().trim();
    const email = this.email().trim();
    this.enviando.set(true);
    this.api
      .registrarReclamo(
        {
          tipo: this.tipo(),
          consumidor: {
            nombres: this.nombres().trim(),
            apellidos: this.apellidos().trim(),
            tipoDocumento: this.tipoDocumento() as 'DNI' | 'CE',
            numeroDocumento: this.numeroDocumento().trim(),
            domicilio: this.domicilio().trim(),
            telefono: this.telefono().trim(),
            email,
            esMenor: this.esMenor(),
            apoderadoNombre: this.esMenor() ? this.apoderado().trim() : null,
          },
          bien: {
            tipo: this.bienTipo() as 'PRODUCTO' | 'SERVICIO',
            descripcion: this.bienDescripcion().trim(),
            montoReclamado: monto.length > 0 ? Number(monto) : null,
            pedidoCodigo: pedido.length > 0 ? pedido : null,
          },
          detalle: this.detalle().trim(),
          pedidoConsumidor: this.pedidoConsumidor().trim(),
        },
        'body',
        false,
        { context: new HttpContext().set(SIN_TOAST, true) },
      )
      .subscribe({
        next: (respuesta) => {
          this.enviando.set(false);
          const datos = respuesta.data!;
          this.enviado.emit({ numero: datos.numero, fechaLimite: datos.fechaLimite, email });
        },
        error: (e: unknown) => {
          this.enviando.set(false);
          this.mostrarError(e);
        },
      });
  }

  private mostrarError(e: unknown): void {
    const porCampo: Partial<Record<Campo, string>> = {};
    for (const { campo, mensaje } of camposConError(e)) {
      const destino = CAMPO_DEL_SERVIDOR[campo];
      if (destino) porCampo[destino] = mensaje;
    }
    this.erroresServidor.set(porCampo);
    const estado = e instanceof HttpErrorResponse ? e.status : 0;
    if (estado === 429) {
      this.errorEnvio.set('Demasiados intentos, espera un minuto antes de enviar otro reclamo.');
    } else if (estado === 400) {
      this.errorEnvio.set('Revisa los datos marcados e inténtalo de nuevo.');
    } else {
      this.errorEnvio.set('No pudimos registrar tu reclamo. Inténtalo en un momento.');
    }
  }
}
