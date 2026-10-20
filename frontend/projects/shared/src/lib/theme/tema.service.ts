import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import type { ConfiguracionPublica } from '../api/settings-service';
import { ConfiguracionPublicaService } from '../api/settings-service';
import { SIN_TOAST } from '../http/interceptores';
import { TEMA_FITSTORE, derivarColores, type Tema } from './colores';

/** Clave de localStorage con la última configuración pública (respaldo para CA-0b-07). */
export const CLAVE_RESPALDO_CONFIG = 'fs_config_publica';

/** Si settings tarda más de esto, la app arranca igual con el respaldo o el tema por defecto. */
const ESPERA_MAXIMA_MS = 3000;

const NOMBRES_VARIABLES: Record<keyof Tema, string> = {
  colorPrimario: '--fs-color-primario',
  colorSecundario: '--fs-color-secundario',
  colorAcento: '--fs-color-acento',
  colorFondo: '--fs-color-fondo',
  colorTexto: '--fs-color-texto',
};

/**
 * Marca de la tienda (R4): carga `/settings/public`, escribe los colores como variables CSS en `:root`
 * (los derivados incluidos), guarda una copia en localStorage y pone el título y el favicon.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly api = inject(ConfiguracionPublicaService);
  private readonly titulo = inject(Title);

  private readonly config = signal<ConfiguracionPublica | null>(null);

  /** Configuración pública vigente (la recibida o el respaldo), o `null` si todavía no hay ninguna. */
  readonly configuracion = this.config.asReadonly();
  readonly nombre = computed(() => this.config()?.identidad.nombre ?? null);
  readonly eslogan = computed(() => this.config()?.identidad.eslogan ?? null);
  readonly logoClaroUrl = computed(() => this.config()?.identidad.logoClaroUrl ?? null);
  readonly logoOscuroUrl = computed(() => this.config()?.identidad.logoOscuroUrl ?? null);
  readonly faviconUrl = computed(() => this.config()?.identidad.faviconUrl ?? null);

  /**
   * Aplica de inmediato el respaldo (o el tema FitStore) y luego pide la configuración vigente.
   * La promesa termina cuando llega la respuesta, falla o pasa `ESPERA_MAXIMA_MS`: nunca bloquea la carga.
   */
  async iniciar(): Promise<void> {
    const respaldo = this.leerRespaldo();
    if (respaldo) this.aplicarConfiguracion(respaldo);
    else this.aplicarTema(TEMA_FITSTORE);

    const contexto = new HttpContext().set(SIN_TOAST, true);
    const carga = firstValueFrom(
      this.api.obtenerConfiguracionPublica('body', false, { context: contexto }),
    )
      .then((respuesta) => {
        if (!respuesta.data) return;
        this.aplicarConfiguracion(respuesta.data);
        this.guardarRespaldo(respuesta.data);
      })
      .catch(() => undefined); // sin respuesta: se queda con el respaldo o el tema por defecto
    let temporizador: ReturnType<typeof setTimeout> | undefined;
    const limite = new Promise<void>(
      (resolver) => (temporizador = setTimeout(resolver, ESPERA_MAXIMA_MS)),
    );
    await Promise.race([carga, limite]);
    clearTimeout(temporizador);
  }

  /** Escribe los cinco colores y sus derivados como variables CSS en `:root`. */
  aplicarTema(tema: Tema): void {
    const estilo = document.documentElement.style;
    for (const clave of Object.keys(NOMBRES_VARIABLES) as (keyof Tema)[]) {
      estilo.setProperty(NOMBRES_VARIABLES[clave], tema[clave].toUpperCase());
    }
    const d = derivarColores(tema);
    estilo.setProperty('--fs-color-en-primario', d.enPrimario);
    estilo.setProperty('--fs-color-en-secundario', d.enSecundario);
    estilo.setProperty('--fs-color-en-acento', d.enAcento);
    estilo.setProperty('--fs-color-primario-texto', d.primarioTexto);
    estilo.setProperty('--fs-color-primario-suave', d.primarioSuave);
    estilo.setProperty('--fs-color-superficie-alterna', d.superficieAlterna);
    estilo.setProperty('--fs-color-texto-suave', d.textoSuave);
    estilo.setProperty('--fs-color-linea', d.linea);
    estilo.setProperty('--fs-color-borde-control', d.bordeControl);
  }

  private aplicarConfiguracion(config: ConfiguracionPublica): void {
    this.config.set(config);
    this.aplicarTema(config.tema);
    this.titulo.setTitle(config.identidad.nombre);
    if (config.identidad.faviconUrl) this.ponerFavicon(config.identidad.faviconUrl);
  }

  private ponerFavicon(url: string): void {
    let enlace = document.head.querySelector<HTMLLinkElement>('link[rel~="icon"]');
    if (!enlace) {
      enlace = document.createElement('link');
      enlace.rel = 'icon';
      document.head.appendChild(enlace);
    }
    enlace.removeAttribute('type'); // el favicon de la tienda puede ser png, ico o webp
    enlace.href = url;
  }

  private leerRespaldo(): ConfiguracionPublica | null {
    try {
      const texto = localStorage.getItem(CLAVE_RESPALDO_CONFIG);
      if (!texto) return null;
      const config = JSON.parse(texto) as ConfiguracionPublica;
      return config?.tema && config.identidad ? config : null;
    } catch {
      return null;
    }
  }

  private guardarRespaldo(config: ConfiguracionPublica): void {
    try {
      localStorage.setItem(CLAVE_RESPALDO_CONFIG, JSON.stringify(config));
    } catch {
      // Sin almacenamiento: la app funciona igual, solo se pierde el respaldo.
    }
  }
}
