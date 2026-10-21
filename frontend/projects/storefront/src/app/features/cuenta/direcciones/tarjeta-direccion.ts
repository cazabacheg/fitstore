import { Component, input, output } from '@angular/core';
import { authApi } from 'shared';
import { FsBoton } from '../../../ui/boton/boton';

/** Una dirección de la libreta (P-1-06) con sus acciones. */
@Component({
  selector: 'fs-tarjeta-direccion',
  imports: [FsBoton],
  template: `
    <article class="tarjeta">
      <header style="display: flex; align-items: center; gap: var(--espacio-2)">
        <h3 style="margin: 0; font-size: 1rem">{{ direccion().alias }}</h3>
        @if (direccion().predeterminada) {
          <span class="fs-insignia fs-insignia--marca">Predeterminada</span>
        }
      </header>
      <p style="margin: var(--espacio-2) 0 0">{{ direccion().direccion }}</p>
      @if (direccion().referencia) {
        <p class="fs-suave" style="margin: 0">{{ direccion().referencia }}</p>
      }
      @if (direccion().telefonoContacto) {
        <p class="fs-suave" style="margin: 0">Contacto: {{ direccion().telefonoContacto }}</p>
      }
      <footer
        style="display: flex; flex-wrap: wrap; gap: var(--espacio-2); margin-top: var(--espacio-3)"
      >
        <button type="button" fsBoton variante="contorno" tamano="sm" (click)="editar.emit()">
          Editar
        </button>
        <button type="button" fsBoton variante="texto" tamano="sm" (click)="eliminar.emit()">
          Eliminar
        </button>
        @if (!direccion().predeterminada) {
          <button type="button" fsBoton variante="texto" tamano="sm" (click)="predeterminar.emit()">
            Marcar como predeterminada
          </button>
        }
      </footer>
    </article>
  `,
  styles: `
    .tarjeta {
      border: 1px solid var(--color-linea);
      border-radius: var(--radius-md);
      padding: var(--espacio-4);
      background: var(--color-superficie);
    }
  `,
})
export class TarjetaDireccion {
  readonly direccion = input.required<authApi.Direccion>();
  readonly editar = output<void>();
  readonly eliminar = output<void>();
  readonly predeterminar = output<void>();
}
