import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FsBoton } from './boton';
import { montar } from '../../testing';

@Component({
  imports: [FsBoton],
  template: `
    <button fsBoton>Guardar</button>
    <button fsBoton variante="peligro" tamano="sm" [bloque]="true" disabled>Cancelar pedido</button>
    <a fsBoton variante="contorno" tamano="lg" href="/x">Ir</a>
  `,
})
class Anfitrion {}

describe('Botón de ui/', () => {
  it('CA-0b-25 la variante primaria es la de por defecto', async () => {
    const { el } = await montar(Anfitrion);
    const b = el.querySelectorAll('button')[0];
    expect(b.classList).toContain('fs-btn');
    expect(b.classList).toContain('fs-btn--primario');
  });

  it('CA-0b-25 acepta variante, tamaño y bloque, y respeta disabled', async () => {
    const { el } = await montar(Anfitrion);
    const b = el.querySelectorAll('button')[1];
    expect(b.classList).toContain('fs-btn--peligro');
    expect(b.classList).toContain('fs-btn--sm');
    expect(b.classList).toContain('fs-btn--bloque');
    expect(b.disabled).toBe(true);
  });

  it('CA-0b-25 también se usa en un enlace', async () => {
    const { el } = await montar(Anfitrion);
    const a = el.querySelector('a')!;
    expect(a.classList).toContain('fs-btn--contorno');
    expect(a.classList).toContain('fs-btn--lg');
    TestBed.resetTestingModule();
  });
});
