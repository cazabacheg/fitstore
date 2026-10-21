import { HttpContext } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import type { Observable } from 'rxjs';
import { SIN_TOAST, SesionStore, ToastService, authApi } from 'shared';
import { EncabezadoPagina } from '../../layout/encabezado-pagina';
import { DialogoService } from '../../ui/dialogo/dialogo.service';
import { EstadoCargando } from '../../ui/estados/estado-cargando';
import { EstadoError } from '../../ui/estados/estado-error';
import { fechaHora } from './formato';

/**
 * Detalle de un cliente (P-1-09): sus datos y direcciones, el enlace "Ver sus pedidos" (CA-1-33) y, solo para un ADMIN,
 * "Bloquear" o "Desbloquear" con confirmación (CA-1-34).
 */
@Component({
  selector: 'fs-cliente-detalle-page',
  imports: [EncabezadoPagina, RouterLink, EstadoCargando, EstadoError],
  template: `
    @switch (estado()) {
      @case ('cargando') {
        <fs-estado-cargando [lineas]="6" />
      }
      @case ('error') {
        <fs-estado-error titulo="No pudimos cargar al cliente" (reintentar)="cargar()" />
      }
      @default {
        @if (cliente(); as c) {
          <fs-encabezado-pagina [titulo]="c.nombres + ' ' + c.apellidos">
            <span accion style="display: flex; gap: var(--espacio-2); flex-wrap: wrap">
              <a
                class="fs-btn fs-btn--contorno"
                routerLink="/pedidos"
                [queryParams]="{ clienteId: c.id }"
                >Ver sus pedidos</a
              >
              @if (esAdmin()) {
                @if (c.activo) {
                  <button type="button" class="fs-btn fs-btn--peligro" (click)="bloquear(c)">
                    Bloquear
                  </button>
                } @else {
                  <button type="button" class="fs-btn fs-btn--primario" (click)="desbloquear(c)">
                    Desbloquear
                  </button>
                }
              }
            </span>
          </fs-encabezado-pagina>
          <section style="margin-top: var(--espacio-4)">
            <dl class="datos">
              <dt>Estado</dt>
              <dd>
                <span
                  class="fs-insignia"
                  [class]="'fs-insignia fs-insignia--' + (c.activo ? 'exito' : 'error')"
                  >{{ c.activo ? 'Activo' : 'Bloqueado' }}</span
                >
              </dd>
              <dt>Correo</dt>
              <dd>{{ c.email }}</dd>
              <dt>DNI</dt>
              <dd>{{ c.dni }}</dd>
              <dt>Celular</dt>
              <dd>{{ c.telefono }}</dd>
              <dt>Registrado</dt>
              <dd>{{ fecha(c.creadoEn) }}</dd>
              <dt>Último ingreso</dt>
              <dd>{{ fecha(c.ultimoLoginEn) }}</dd>
            </dl>
          </section>
          <h2 style="margin: var(--espacio-6) 0 var(--espacio-3)">Direcciones</h2>
          @if (c.direcciones.length === 0) {
            <p class="fs-suave">Aún no tiene direcciones guardadas.</p>
          }
          <div style="display: grid; gap: var(--espacio-3); max-width: 640px">
            @for (d of c.direcciones; track d.id) {
              <article class="direccion">
                <strong>{{ d.alias }}</strong>
                @if (d.predeterminada) {
                  <span class="fs-insignia fs-insignia--marca">Predeterminada</span>
                }
                <div>{{ d.direccion }}</div>
                @if (d.referencia) {
                  <div class="fs-suave">{{ d.referencia }}</div>
                }
                @if (d.telefonoContacto) {
                  <div class="fs-suave">Contacto: {{ d.telefonoContacto }}</div>
                }
              </article>
            }
          </div>
        }
      }
    }
  `,
  styles: `
    .datos {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: var(--espacio-2) var(--espacio-5);
      margin: 0;
    }
    .datos dt {
      color: var(--color-texto-suave);
    }
    .datos dd {
      margin: 0;
    }
    .direccion {
      border: 1px solid var(--color-linea);
      border-radius: var(--radius-md);
      padding: var(--espacio-3) var(--espacio-4);
    }
  `,
})
export class ClienteDetallePage {
  private readonly api = inject(authApi.ClientesAdminService);
  private readonly confirmacion = inject(DialogoService);
  private readonly toasts = inject(ToastService);
  private readonly sesion = inject(SesionStore);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';

  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly cliente = signal<authApi.ClienteDetalle | null>(null);
  /** Solo un ADMIN bloquea (el OPERADOR no ve el botón y la API le respondería 403). */
  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly fecha = fechaHora;

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api
      .obtenerCliente(this.id, 'body', false, { context: new HttpContext().set(SIN_TOAST, true) })
      .subscribe({
        next: (respuesta) => {
          this.cliente.set(respuesta.data ?? null);
          this.estado.set(respuesta.data ? 'listo' : 'error');
        },
        error: () => this.estado.set('error'),
      });
  }

  protected bloquear(c: authApi.ClienteDetalle): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Bloquear a ${c.nombres} ${c.apellidos}?`,
        mensaje: 'No podrá ingresar ni refrescar su sesión. Puedes desbloquearlo cuando quieras.',
        accion: 'Bloquear cliente',
        peligro: true,
      })
      .subscribe((confirmado) => {
        if (confirmado) this.aplicar(this.api.bloquearCliente(c.id), 'Cliente bloqueado');
      });
  }

  protected desbloquear(c: authApi.ClienteDetalle): void {
    this.confirmacion
      .confirmar({
        titulo: `¿Desbloquear a ${c.nombres} ${c.apellidos}?`,
        accion: 'Desbloquear cliente',
      })
      .subscribe((confirmado) => {
        if (confirmado) this.aplicar(this.api.desbloquearCliente(c.id), 'Cliente desbloqueado');
      });
  }

  private aplicar(llamada: Observable<authApi.RespuestaClienteDetalle>, aviso: string): void {
    llamada.subscribe({
      next: (respuesta) => {
        if (respuesta.data) this.cliente.set(respuesta.data);
        this.toasts.mostrar('exito', aviso);
      },
      error: () => undefined, // el interceptor ya mostró el motivo
    });
  }
}
