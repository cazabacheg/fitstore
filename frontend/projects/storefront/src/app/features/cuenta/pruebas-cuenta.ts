import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideFitstoreHttp, provideSesionConfig } from 'shared';

export const ok = <T>(data: T, mensaje = 'Operación exitosa') => ({ success: true, mensaje, data });
export const error400 = (campos: { campo: string; mensaje: string }[]) => ({
  success: false,
  mensaje: 'Revisa los datos ingresados',
  codigo: 'VALIDACION',
  errores: campos,
});

export const PROVEEDORES = [
  provideRouter([]),
  provideFitstoreHttp({ carrito: false }),
  provideHttpClientTesting(),
  provideSesionConfig({ tipo: 'CLIENTE' }),
];

export const http = () => TestBed.inject(HttpTestingController);
