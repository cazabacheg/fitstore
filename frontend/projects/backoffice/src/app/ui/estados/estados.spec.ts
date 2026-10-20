import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EstadoCargando } from './estado-cargando';
import { EstadoError } from './estado-error';
import { EstadoVacio } from './estado-vacio';

@Component({
  imports: [EstadoCargando, EstadoVacio, EstadoError],
  template: `
    <fs-estado-cargando [lineas]="3" />
    <fs-estado-vacio titulo="Aún no hay pedidos" texto="Cuando entren pedidos aparecerán aquí.">
      <button class="accion-vacio">Crear</button>
    </fs-estado-vacio>
    <fs-estado-error texto="No pudimos cargar los pedidos." (reintentar)="reintentos = reintentos + 1" />
  `,
})
class Anfitrion {
  reintentos = 0;
}

describe('Estados comunes (P-0b-03)', () => {
  it('CA-0b-25 EstadoCargando dibuja un esqueleto con las líneas pedidas y se anuncia como cargando', () => {
    const f = TestBed.createComponent(Anfitrion);
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    const cargando = el.querySelector('[aria-label="Cargando"]') as HTMLElement;
    expect(cargando.getAttribute('role')).toBe('status');
    expect(cargando.querySelectorAll('.fs-esqueleto').length).toBe(3);
  });

  it('CA-0b-25 EstadoVacio dice qué falta y deja poner la acción', () => {
    const f = TestBed.createComponent(Anfitrion);
    f.detectChanges();
    const vacio = (f.nativeElement as HTMLElement).querySelector('fs-estado-vacio') as HTMLElement;
    expect(vacio.textContent).toContain('Aún no hay pedidos');
    expect(vacio.textContent).toContain('Cuando entren pedidos aparecerán aquí.');
    expect(vacio.querySelector('.accion-vacio')).not.toBeNull();
  });

  it('CA-0b-25 EstadoError muestra el mensaje y "Reintentar" emite el evento', () => {
    const f = TestBed.createComponent(Anfitrion);
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    expect(el.querySelector('fs-estado-error')?.textContent).toContain('No pudimos cargar los pedidos.');
    const boton = [...el.querySelectorAll('button')].find((b) => b.textContent?.includes('Reintentar')) as HTMLElement;
    boton.click();
    expect(f.componentInstance.reintentos).toBe(1);
  });
});
