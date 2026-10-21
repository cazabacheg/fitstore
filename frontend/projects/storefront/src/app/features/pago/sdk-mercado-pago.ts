import { InjectionToken } from '@angular/core';

/** Un Brick de Mercado Pago ya montado en la página. */
export interface BrickCreado {
  unmount(): void;
}

/** Lo que la página usa del SDK de Mercado Pago (`MercadoPago(clavePublica).bricks().create(...)`). */
export interface SdkMercadoPago {
  crear(clavePublica: string): {
    bricks(): {
      create(tipo: string, idContenedor: string, config: Record<string, unknown>): Promise<BrickCreado>;
    };
  };
}

const URL_SDK = 'https://sdk.mercadopago.com/js/v2';

interface VentanaMercadoPago {
  MercadoPago?: new (clavePublica: string, opciones?: Record<string, unknown>) => ReturnType<SdkMercadoPago['crear']>;
}

/** Carga el script de Mercado Pago solo en esta ruta (la CSP de la spec 8 permite `sdk.mercadopago.com`). */
function cargarSdkReal(): Promise<SdkMercadoPago> {
  const ventana = window as unknown as VentanaMercadoPago;
  const crear = (): SdkMercadoPago => ({
    crear: (clavePublica) => new ventana.MercadoPago!(clavePublica, { locale: 'es-PE' }),
  });
  if (ventana.MercadoPago) return Promise.resolve(crear());
  return new Promise((resolver, rechazar) => {
    const script = document.createElement('script');
    script.src = URL_SDK;
    script.async = true;
    script.onload = () => resolver(crear());
    script.onerror = () => rechazar(new Error('No se pudo cargar Mercado Pago'));
    document.head.append(script);
  });
}

/** Cargador del SDK; las pruebas lo reemplazan por uno falso. */
export const SDK_MERCADO_PAGO = new InjectionToken<() => Promise<SdkMercadoPago>>('SDK_MERCADO_PAGO', {
  providedIn: 'root',
  factory: () => cargarSdkReal,
});
