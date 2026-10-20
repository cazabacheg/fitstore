import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { montar } from '../../testing';
import { UiCatalogo } from './ui-catalogo';

describe('Catálogo /_ui de la tienda', () => {
  it('CA-0b-25 muestra cada componente de ui/ con sus variantes y estados', async () => {
    const { el } = await montar(UiCatalogo, {}, [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
    ]);
    TestBed.inject(HttpTestingController)
      .match(() => true)
      .forEach((r) => r.flush({ success: true, mensaje: '', data: [] }));
    for (const seccion of [
      'Botón',
      'Campo',
      'Select',
      'Checkbox',
      'Selector de ubigeo',
      'Precio',
      'Tarjeta de producto',
      'Badge',
      'Paginador',
      'Galería',
      'Estados',
      'Toast',
      'Diálogo',
    ]) {
      expect(el.textContent, seccion).toContain(seccion);
    }
    // Variantes del botón y del badge.
    for (const v of ['primario', 'contorno', 'acento', 'secundario', 'texto', 'peligro']) {
      expect(el.querySelector(`.fs-btn--${v}`), v).not.toBeNull();
    }
    for (const v of ['exito', 'aviso', 'error', 'info', 'marca', 'acento']) {
      expect(el.querySelector(`.fs-insignia--${v}`), v).not.toBeNull();
    }
    // Estados del campo y de la vista.
    expect(el.querySelector('.fs-campo--error')).not.toBeNull();
    expect(el.querySelector('.fs-esqueleto')).not.toBeNull();
    expect(el.querySelector('.fs-estado--error')).not.toBeNull();
    expect(el.querySelector('.fs-tarjeta-producto--agotado')).not.toBeNull();
  });
});
