import { Component, inject, signal } from '@angular/core';
import { ToastService } from 'shared';
import { FsBadge } from '../../ui/badge/badge';
import { FsBoton } from '../../ui/boton/boton';
import { FsCampo } from '../../ui/campo/campo';
import { FsCheckbox } from '../../ui/checkbox/checkbox';
import { DialogoConfirmacion } from '../../ui/dialogo/dialogo-confirmacion';
import { EstadoCargando, EstadoError, EstadoVacio } from '../../ui/estados/estados';
import { FsGaleria } from '../../ui/galeria/galeria';
import { FsPaginador } from '../../ui/paginador/paginador';
import { FsPrecio } from '../../ui/precio/precio';
import { FsSelect } from '../../ui/select/select';
import { FsSelectorUbigeo } from '../../ui/selector-ubigeo/selector-ubigeo';
import { FsTarjetaProducto } from '../../ui/tarjeta-producto/tarjeta-producto';

/** Catálogo `/_ui` (P-0b-04): cada componente de `ui/` con sus variantes y estados. Solo en desarrollo. */
@Component({
  selector: 'fs-ui-catalogo',
  imports: [
    FsBoton,
    FsCampo,
    FsSelect,
    FsCheckbox,
    FsSelectorUbigeo,
    FsPrecio,
    FsTarjetaProducto,
    FsBadge,
    FsPaginador,
    FsGaleria,
    EstadoCargando,
    EstadoVacio,
    EstadoError,
    DialogoConfirmacion,
  ],
  template: `
    <div class="fs-pagina" style="padding-block: 32px; display: grid; gap: 40px">
      <h1 class="fs-titulo-pagina" style="margin: 0; font-size: 28px; font-weight: 700">
        Componentes de la tienda
      </h1>

      <section class="fs-col" aria-labelledby="ui-boton">
        <h2 id="ui-boton" class="fs-estado__titulo">Botón</h2>
        <div class="fs-fila">
          @for (v of variantes; track v) {
            <button type="button" fsBoton [variante]="v">{{ v }}</button>
          }
          <button type="button" fsBoton variante="primario" tamano="sm">Pequeño</button>
          <button type="button" fsBoton variante="primario" tamano="lg">Grande</button>
          <button type="button" fsBoton variante="primario" disabled>Deshabilitado</button>
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-campo">
        <h2 id="ui-campo" class="fs-estado__titulo">Campo</h2>
        <div class="fs-ubigeo">
          <fs-campo etiqueta="Nombre" placeholder="Rosa Huamán" ayuda="Como figura en tu DNI" />
          <fs-campo etiqueta="Correo" valor="rosa@" error="Escribe un correo válido" />
          <fs-campo etiqueta="Código" [deshabilitado]="true" valor="FS-000124" />
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-select">
        <h2 id="ui-select" class="fs-estado__titulo">Select</h2>
        <div class="fs-ubigeo">
          <fs-select etiqueta="Talla" placeholder="Elige una talla" [opciones]="tallas" />
          <fs-select etiqueta="Talla con error" [opciones]="tallas" error="Elige una talla" />
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-checkbox">
        <h2 id="ui-checkbox" class="fs-estado__titulo">Checkbox</h2>
        <div class="fs-fila">
          <fs-checkbox etiqueta="Recibir ofertas" />
          <fs-checkbox etiqueta="Marcado" [marcado]="true" />
          <fs-checkbox etiqueta="Deshabilitado" [deshabilitado]="true" />
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-ubigeo">
        <h2 id="ui-ubigeo" class="fs-estado__titulo">Selector de ubigeo</h2>
        <fs-selector-ubigeo (cambio)="ubigeo.set($event)" />
        <p class="fs-suave">
          Código elegido: <span class="fs-codigo">{{ ubigeo() ?? '—' }}</span>
        </p>
      </section>

      <section class="fs-col" aria-labelledby="ui-precio">
        <h2 id="ui-precio" class="fs-estado__titulo">Precio</h2>
        <div class="fs-fila">
          <fs-precio [monto]="59.9" />
          <fs-precio [monto]="53.91" [antes]="59.9" descuento="−10 %" />
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-tarjeta">
        <h2 id="ui-tarjeta" class="fs-estado__titulo">Tarjeta de producto</h2>
        <div class="fs-rejilla-4">
          <fs-tarjeta-producto />
          <fs-tarjeta-producto [producto]="producto" />
          <fs-tarjeta-producto [producto]="agotado" />
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-badge">
        <h2 id="ui-badge" class="fs-estado__titulo">Badge</h2>
        <div class="fs-fila">
          <fs-badge>Cancelado</fs-badge>
          @for (v of variantesBadge; track v) {
            <fs-badge [variante]="v">{{ v }}</fs-badge>
          }
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-paginador">
        <h2 id="ui-paginador" class="fs-estado__titulo">Paginador</h2>
        <fs-paginador [pagina]="pagina()" [totalPaginas]="20" (cambio)="pagina.set($event)" />
      </section>

      <section class="fs-col" aria-labelledby="ui-galeria">
        <h2 id="ui-galeria" class="fs-estado__titulo">Galería</h2>
        <fs-galeria [imagenes]="[]" />
      </section>

      <section class="fs-col" aria-labelledby="ui-estados">
        <h2 id="ui-estados" class="fs-estado__titulo">Estados</h2>
        <fs-estado-cargando [filas]="3" />
        <fs-estado-vacio
          titulo="Tu carrito está vacío"
          texto="Agrega prendas para empezar"
          accion="Ver productos"
        />
        <fs-estado-error titulo="No pudimos cargar los productos" />
      </section>

      <section class="fs-col" aria-labelledby="ui-toast">
        <h2 id="ui-toast" class="fs-estado__titulo">Toast</h2>
        <div class="fs-fila">
          <button
            type="button"
            fsBoton
            variante="contorno"
            (click)="avisar('exito', 'Agregado al carrito')"
          >
            Éxito
          </button>
          <button
            type="button"
            fsBoton
            variante="contorno"
            (click)="avisar('info', 'Actualizamos tu carrito')"
          >
            Información
          </button>
          <button
            type="button"
            fsBoton
            variante="contorno"
            (click)="avisar('error', 'Sin conexión, inténtalo de nuevo')"
          >
            Error
          </button>
        </div>
      </section>

      <section class="fs-col" aria-labelledby="ui-dialogo">
        <h2 id="ui-dialogo" class="fs-estado__titulo">Diálogo</h2>
        <div class="fs-fila">
          <button type="button" fsBoton variante="peligro" (click)="dialogo.set(true)">
            Cancelar pedido
          </button>
        </div>
        <fs-dialogo-confirmacion
          [(abierto)]="dialogo"
          titulo="¿Cancelar el pedido FS-000124?"
          texto="Esta acción no se puede deshacer."
          textoConfirmar="Cancelar pedido"
          [peligro]="true"
        />
      </section>
    </div>
  `,
})
export class UiCatalogo {
  private readonly toasts = inject(ToastService);

  protected readonly variantes = [
    'primario',
    'contorno',
    'acento',
    'secundario',
    'texto',
    'peligro',
  ] as const;
  protected readonly variantesBadge = [
    'exito',
    'aviso',
    'error',
    'info',
    'marca',
    'acento',
  ] as const;
  protected readonly tallas = [
    { valor: 'S', etiqueta: 'S' },
    { valor: 'M', etiqueta: 'M' },
    { valor: 'L', etiqueta: 'L' },
  ];
  protected readonly producto = {
    id: 1,
    slug: 'legging-negro',
    nombre: 'Legging negro',
    imagenUrl: null,
    precioDesde: 59.9,
    colores: [{ id: 1, nombre: 'Negro', hex: '#222222', activo: true }],
    etiquetaDescuento: '−10 %',
    agotado: false,
  };
  protected readonly agotado = {
    ...this.producto,
    id: 2,
    slug: 'top-blanco',
    nombre: 'Top blanco',
    etiquetaDescuento: null,
    agotado: true,
  };

  protected readonly ubigeo = signal<string | null>(null);
  protected readonly pagina = signal(9);
  protected readonly dialogo = signal(false);

  protected avisar(tipo: 'exito' | 'info' | 'error', mensaje: string): void {
    this.toasts.mostrar(tipo, mensaje);
  }
}
