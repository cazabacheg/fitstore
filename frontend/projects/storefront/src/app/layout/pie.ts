import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TemaService } from 'shared';

const POLITICAS = [
  { tipo: 'envios', etiqueta: 'Envíos', clave: 'envios', columna: 'ayuda' },
  {
    tipo: 'cambios-devoluciones',
    etiqueta: 'Cambios y devoluciones',
    clave: 'cambiosDevoluciones',
    columna: 'ayuda',
  },
  { tipo: 'terminos', etiqueta: 'Términos y condiciones', clave: 'terminos', columna: 'legal' },
  { tipo: 'privacidad', etiqueta: 'Política de privacidad', clave: 'privacidad', columna: 'legal' },
] as const;

const MEDIOS_PAGO: Record<string, string> = {
  CULQI: 'Culqi',
  YAPE: 'Yape',
  MERCADO_PAGO: 'Mercado Pago',
};

/** Pie de la tienda, al final del documento y nunca fijo (P-0b-01, TiendaPie). */
@Component({
  selector: 'fs-pie',
  imports: [RouterLink],
  template: `
    <footer class="fs-pie">
      <div class="fs-pagina fs-rejilla-4 fs-pie__rejilla" style="padding-block: 48px 32px">
        <div class="fs-pie__col">
          <span class="fs-pie__marca">{{ nombre() }}</span>
          @if (config()?.identidad?.eslogan) {
            <span class="fs-pie__suave">{{ config()?.identidad?.eslogan }}</span>
          }
          @if (config()?.contenido?.tiendaFisica; as tienda) {
            <span class="fs-pie__suave">{{ tienda.direccion }}</span>
          }
        </div>
        <nav class="fs-pie__col" aria-label="Ayuda">
          <span class="fs-pie__titulo">Ayuda</span>
          @for (p of ayuda(); track p.tipo) {
            <a [routerLink]="['/politicas', p.tipo]">{{ p.etiqueta }}</a>
          }
        </nav>
        <nav class="fs-pie__col" aria-label="Legal">
          <span class="fs-pie__titulo">Legal</span>
          @for (p of legal(); track p.tipo) {
            <a [routerLink]="['/politicas', p.tipo]">{{ p.etiqueta }}</a>
          }
          <a routerLink="/libro-de-reclamaciones" class="fs-pie__libro">
            <span class="fs-icono" aria-hidden="true">menu_book</span>Libro de Reclamaciones
          </a>
        </nav>
        <div class="fs-pie__col">
          <span class="fs-pie__titulo">Contacto</span>
          @if (config()?.contenido?.whatsapp; as w) {
            <span>WhatsApp {{ w }}</span>
          }
          @if (config()?.contenido?.telefono; as t) {
            <span>Tel. {{ t }}</span>
          }
          @if (config()?.contenido?.email; as e) {
            <span>{{ e }}</span>
          }
          @if (config()?.contenido?.horario; as h) {
            <span class="fs-pie__suave">{{ h }}</span>
          }
          <div class="fs-pie__redes">
            @for (r of redes(); track r.nombre) {
              <a [href]="r.url" target="_blank" rel="noopener noreferrer">{{ r.nombre }}</a>
            }
          </div>
        </div>
      </div>
      <div class="fs-pagina fs-pie__base">
        @if (config()?.identidad; as id) {
          <span>{{ id.razonSocial }} · RUC {{ id.ruc }}</span>
        }
        @if (pagos()) {
          <span>{{ pagos() }}</span>
        }
      </div>
    </footer>
  `,
})
export class Pie {
  private readonly tema = inject(TemaService);

  protected readonly config = this.tema.configuracion;
  protected readonly nombre = computed(() => this.tema.nombre() ?? 'FitStore');

  private readonly politicas = computed(() => {
    const versiones = this.config()?.politicas;
    // Sin configuración se ofrecen las cuatro (SPEC-3); con ella, solo las que tienen versión publicada.
    return POLITICAS.filter((p) => !versiones || versiones[p.clave] > 0);
  });
  protected readonly ayuda = computed(() => this.politicas().filter((p) => p.columna === 'ayuda'));
  protected readonly legal = computed(() => this.politicas().filter((p) => p.columna === 'legal'));

  protected readonly redes = computed(() => {
    const c = this.config()?.contenido;
    return [
      { nombre: 'Instagram', url: c?.instagramUrl },
      { nombre: 'Facebook', url: c?.facebookUrl },
      { nombre: 'TikTok', url: c?.tiktokUrl },
    ].filter((r): r is { nombre: string; url: string } => !!r.url);
  });

  /** "Pagos seguros con Culqi y Mercado Pago", con los medios habilitados; vacío si no hay ninguno. */
  protected readonly pagos = computed(() => {
    const medios = Array.from(this.config()?.parametros?.metodosPago ?? []).map(
      (m) => MEDIOS_PAGO[m] ?? m,
    );
    if (!medios.length) return '';
    return `Pagos seguros con ${new Intl.ListFormat('es', { style: 'long', type: 'conjunction' }).format(medios)}`;
  });
}
