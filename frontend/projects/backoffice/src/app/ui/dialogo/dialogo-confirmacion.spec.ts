import { OverlayContainer } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { DialogoService } from './dialogo.service';

function panel(): HTMLElement {
  return TestBed.inject(OverlayContainer).getContainerElement();
}
const boton = (texto: string) =>
  [...panel().querySelectorAll<HTMLElement>('button')].find((b) => b.textContent?.trim() === texto) as HTMLElement;
const abrir = () =>
  TestBed.inject(DialogoService).confirmar({
    titulo: '¿Cancelar el pedido FS-000124?',
    mensaje: 'El stock reservado se libera.',
    accion: 'Cancelar pedido',
  });

describe('DialogoConfirmacion (ui)', () => {
  it('CA-0b-25 muestra el título, el mensaje, "Volver" y el verbo exacto de la acción', async () => {
    const resultado = abrir();
    const sub = resultado.subscribe();
    await Promise.resolve();
    expect(panel().textContent).toContain('¿Cancelar el pedido FS-000124?');
    expect(panel().textContent).toContain('El stock reservado se libera.');
    const textos = [...panel().querySelectorAll('button')].map((b) => b.textContent?.trim());
    expect(textos).toEqual(['Volver', 'Cancelar pedido']);
    sub.unsubscribe();
  });

  it('CA-0b-25 confirmar devuelve true y "Volver" devuelve false', async () => {
    const si = firstValueFrom(abrir());
    await Promise.resolve();
    boton('Cancelar pedido').click();
    expect(await si).toBe(true);

    const no = firstValueFrom(abrir());
    await Promise.resolve();
    boton('Volver').click();
    expect(await no).toBe(false);
  });

  it('CA-0b-25 Esc cierra el diálogo sin confirmar', async () => {
    const r = firstValueFrom(abrir());
    await Promise.resolve();
    panel().querySelector('mat-dialog-container')?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(await r).toBe(false);
  });
});
