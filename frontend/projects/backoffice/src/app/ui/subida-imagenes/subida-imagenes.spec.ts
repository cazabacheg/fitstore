import { CdkDropList } from '@angular/cdk/drag-drop';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { SubidaImagenes } from './subida-imagenes';

@Component({
  imports: [SubidaImagenes],
  template: `<fs-subida-imagenes
    [tipos]="['image/jpeg', 'image/png', 'image/webp']"
    [tamanoMaximoMb]="2"
    [maximoArchivos]="3"
    (archivosChange)="lotes.push($event)"
  />`,
})
class Anfitrion {
  lotes: File[][] = [];
}

const archivo = (nombre: string, tipo: string, bytes = 1000) =>
  new File([new Uint8Array(bytes)], nombre, { type: tipo });

function montar() {
  URL.createObjectURL = vi.fn(() => 'blob:vista-previa');
  URL.revokeObjectURL = vi.fn();
  const f = TestBed.createComponent(Anfitrion);
  f.detectChanges();
  const el = f.nativeElement as HTMLElement;
  const zona = el.querySelector('.fs-subida') as HTMLElement;
  const soltar = (...archivos: File[]) => {
    const evento = new Event('drop', { bubbles: true, cancelable: true });
    Object.defineProperty(evento, 'dataTransfer', { value: { files: archivos } });
    zona.dispatchEvent(evento);
    f.detectChanges();
  };
  return { f, el, zona, soltar };
}
const miniaturas = (el: HTMLElement) => el.querySelectorAll('.fs-subida__item');
const motivos = (el: HTMLElement) =>
  [...el.querySelectorAll('.fs-subida__rechazo')].map((n) =>
    n.textContent?.replace(/\s+/g, ' ').trim(),
  );

describe('SubidaImagenes (ui)', () => {
  it('CA-0b-25 al arrastrar sobre la zona cambia el borde y al soltar acepta las imágenes con vista previa', () => {
    const { f, el, zona, soltar } = montar();
    zona.dispatchEvent(new Event('dragover', { cancelable: true }));
    f.detectChanges();
    expect(zona.classList.contains('fs-subida--arrastrando')).toBe(true);
    soltar(archivo('frente.jpg', 'image/jpeg'), archivo('espalda.png', 'image/png'));
    expect(zona.classList.contains('fs-subida--arrastrando')).toBe(false);
    expect(miniaturas(el).length).toBe(2);
    expect(el.querySelector('.fs-subida__item img')?.getAttribute('src')).toBe('blob:vista-previa');
    expect(f.componentInstance.lotes.at(-1)?.map((a) => a.name)).toEqual([
      'frente.jpg',
      'espalda.png',
    ]);
  });

  it('CA-0b-25 rechaza un tipo no permitido, un SVG y un archivo muy pesado, diciendo el nombre y el motivo', () => {
    const { el, soltar } = montar();
    soltar(
      archivo('logo.svg', 'image/svg+xml'),
      archivo('foto.gif', 'image/gif'),
      archivo('enorme.jpg', 'image/jpeg', 3 * 1024 * 1024),
    );
    expect(miniaturas(el).length).toBe(0);
    expect(motivos(el)).toEqual([
      'logo.svg: solo se aceptan imágenes JPG, PNG o WebP.',
      'foto.gif: solo se aceptan imágenes JPG, PNG o WebP.',
      'enorme.jpg: pesa más de 2 MB.',
    ]);
  });

  it('CA-0b-25 respeta el máximo de archivos', () => {
    const { el, soltar } = montar();
    soltar(
      archivo('1.jpg', 'image/jpeg'),
      archivo('2.jpg', 'image/jpeg'),
      archivo('3.jpg', 'image/jpeg'),
      archivo('4.jpg', 'image/jpeg'),
    );
    expect(miniaturas(el).length).toBe(3);
    expect(motivos(el)).toEqual(['4.jpg: el máximo es de 3 imágenes.']);
  });

  it('CA-0b-25 cada miniatura se quita con su botón', () => {
    const { f, el, soltar } = montar();
    soltar(archivo('frente.jpg', 'image/jpeg'), archivo('espalda.png', 'image/png'));
    (el.querySelector('button[aria-label="Quitar frente.jpg"]') as HTMLElement).click();
    f.detectChanges();
    expect(miniaturas(el).length).toBe(1);
    expect(f.componentInstance.lotes.at(-1)?.map((a) => a.name)).toEqual(['espalda.png']);
    expect(URL.revokeObjectURL).toHaveBeenCalled();
  });

  it('CA-0b-25 las miniaturas se reordenan arrastrando', () => {
    const { f, el, soltar } = montar();
    soltar(archivo('frente.jpg', 'image/jpeg'), archivo('espalda.png', 'image/png'));
    const lista = f.debugElement.query(By.directive(CdkDropList)).injector.get(CdkDropList);
    lista.dropped.emit({ previousIndex: 0, currentIndex: 1 } as never);
    f.detectChanges();
    expect(f.componentInstance.lotes.at(-1)?.map((a) => a.name)).toEqual([
      'espalda.png',
      'frente.jpg',
    ]);
    expect(miniaturas(el).length).toBe(2);
  });

  it('CA-0b-25 elegir archivos con el botón también los valida y acepta', () => {
    const { f, el } = montar();
    const input = el.querySelector('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', {
      value: [archivo('lateral.webp', 'image/webp')],
      configurable: true,
    });
    input.dispatchEvent(new Event('change'));
    f.detectChanges();
    expect(miniaturas(el).length).toBe(1);
    expect(input.getAttribute('accept')).toBe('image/jpeg,image/png,image/webp');
  });
});
