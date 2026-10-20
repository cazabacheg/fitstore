import { TEMA_FITSTORE } from 'shared';
import { configuracionAdmin, escribir, montarComponente, porId } from './datos-prueba';
import { EditorTema } from './editor-tema';

const TEMA_ACTUAL = configuracionAdmin().tema;

function montar(extra: Record<string, unknown> = {}) {
  return montarComponente(EditorTema, { tema: TEMA_ACTUAL, ...extra });
}

const variable = (el: HTMLElement, nombre: string) =>
  porId(el, 'vista-previa').style.getPropertyValue(nombre).toUpperCase();

describe('EditorTema (P-3-04, pestaña Tema)', () => {
  it('CA-3-09 la vista previa arranca con los colores guardados', async () => {
    const { el } = await montar();
    expect(variable(el, '--fs-color-primario')).toBe('#C9B28C');
    expect(variable(el, '--fs-color-fondo')).toBe('#FAF9F7');
  });

  it('CA-3-09 al cambiar un color la vista previa se actualiza al instante, sin guardar', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as EditorTema).guardar.subscribe((t) => guardadas.push(t));

    escribir(porId(el, 'color-primario'), '#1E3A8A');
    await estabilizar();

    expect(variable(el, '--fs-color-primario')).toBe('#1E3A8A');
    expect(guardadas).toEqual([]);
  });

  it('CA-3-09 el selector de color y el campo de texto van juntos', async () => {
    const { el, estabilizar } = await montar();
    escribir(porId(el, 'selector-secundario'), '#112233');
    await estabilizar();
    expect((porId(el, 'color-secundario') as HTMLInputElement).value.toUpperCase()).toBe('#112233');
    expect(variable(el, '--fs-color-secundario')).toBe('#112233');
  });

  it('CA-3-09 la vista previa se puede ver como tienda o backoffice, en escritorio o móvil', async () => {
    const { el, estabilizar } = await montar();
    const previa = porId(el, 'vista-previa');
    expect(previa.classList.contains('vp--tienda')).toBe(true);
    expect(previa.classList.contains('vp--escritorio')).toBe(true);
    porId<HTMLButtonElement>(el, 'app-backoffice').click();
    porId<HTMLButtonElement>(el, 'disp-movil').click();
    await estabilizar();
    expect(previa.classList.contains('vp--backoffice')).toBe(true);
    expect(previa.classList.contains('vp--movil')).toBe(true);
  });

  it('CA-3-09 la vista previa no toca el tema real de la página', async () => {
    const { el, estabilizar } = await montar();
    document.documentElement.style.setProperty('--fs-color-primario', '#C9B28C');
    escribir(porId(el, 'color-primario'), '#FF0000');
    await estabilizar();
    expect(document.documentElement.style.getPropertyValue('--fs-color-primario')).toBe('#C9B28C');
  });

  it('CA-3-10 un contraste texto/fondo menor a 4.5:1 muestra la advertencia con la razón calculada y deja guardar', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as EditorTema).guardar.subscribe((t) => guardadas.push(t));
    escribir(porId(el, 'color-texto'), '#9A9A9A');
    escribir(porId(el, 'color-fondo'), '#FFFFFF');
    await estabilizar();

    const avisos = [...el.querySelectorAll('[data-testid="advertencia"]')].map(
      (a) => a.textContent,
    );
    expect(avisos.some((a) => a?.includes('Contraste texto/fondo 2.8:1'))).toBe(true);

    const guardar = porId<HTMLButtonElement>(el, 'guardar-tema');
    expect(guardar.disabled).toBe(false);
    guardar.click();
    expect(guardadas).toHaveLength(1);
  });

  it('CA-3-10 avisa si ni el blanco ni el color de texto se leen sobre el primario', async () => {
    const { el, estabilizar } = await montar();
    escribir(porId(el, 'color-primario'), '#808080');
    escribir(porId(el, 'color-texto'), '#777777');
    escribir(porId(el, 'color-fondo'), '#FFFFFF');
    await estabilizar();
    const avisos = [...el.querySelectorAll('[data-testid="advertencia"]')].map(
      (a) => a.textContent ?? '',
    );
    expect(avisos.some((a) => a.includes('color primario'))).toBe(true);
  });

  it('CA-3-10 el tema por defecto de FitStore no muestra advertencias', async () => {
    const { el } = await montar();
    expect(el.querySelectorAll('[data-testid="advertencia"]').length).toBe(0);
  });

  it('CA-3-10 muestra también las advertencias que devolvió el servidor al guardar', async () => {
    const { el } = await montar({
      advertenciasServidor: ['Contraste texto/fondo 3.1:1 (recomendado ≥ 4.5:1)'],
    });
    expect(el.textContent).toContain('Contraste texto/fondo 3.1:1');
  });

  it('CA-3-11 guardar emite los cinco colores escritos', async () => {
    const { el, estabilizar, comp } = await montar();
    const guardadas: unknown[] = [];
    (comp as EditorTema).guardar.subscribe((t) => guardadas.push(t));
    escribir(porId(el, 'color-acento'), '#336699');
    await estabilizar();
    porId<HTMLButtonElement>(el, 'guardar-tema').click();
    expect(guardadas).toEqual([{ ...TEMA_ACTUAL, colorAcento: '#336699' }]);
  });

  it('CA-3-11 un color con formato inválido se marca y no se puede guardar', async () => {
    const { el, estabilizar } = await montar();
    escribir(porId(el, 'color-primario'), 'rojo');
    await estabilizar();
    expect(porId(el, 'color-primario').getAttribute('aria-invalid')).toBe('true');
    expect(porId<HTMLButtonElement>(el, 'guardar-tema').disabled).toBe(true);
  });

  it('CA-3-12 "Restaurar tema por defecto" devuelve el formulario a los valores de FitStore sin guardar', async () => {
    const { el, estabilizar, comp } = await montar({
      tema: { ...TEMA_ACTUAL, colorPrimario: '#1E3A8A', colorTexto: '#111111' },
    });
    const guardadas: unknown[] = [];
    (comp as EditorTema).guardar.subscribe((t) => guardadas.push(t));

    porId<HTMLButtonElement>(el, 'restaurar').click();
    await estabilizar();

    expect((porId(el, 'color-primario') as HTMLInputElement).value.toUpperCase()).toBe(
      TEMA_FITSTORE.colorPrimario,
    );
    expect((porId(el, 'color-texto') as HTMLInputElement).value.toUpperCase()).toBe(
      TEMA_FITSTORE.colorTexto,
    );
    expect(variable(el, '--fs-color-primario')).toBe(TEMA_FITSTORE.colorPrimario);
    expect(guardadas).toEqual([]);
    expect(el.textContent).toContain('sin guardar');
  });

  it('CA-3-12 hasta que se confirme con Guardar, el tema guardado no cambia (aviso de cambios sin guardar)', async () => {
    const { el, estabilizar, comp } = await montar();
    const avisos: boolean[] = [];
    (comp as EditorTema).sinGuardar.subscribe((v) => avisos.push(v));
    escribir(porId(el, 'color-primario'), '#1E3A8A');
    await estabilizar();
    expect(avisos.at(-1)).toBe(true);
    escribir(porId(el, 'color-primario'), TEMA_ACTUAL.colorPrimario);
    await estabilizar();
    expect(avisos.at(-1)).toBe(false);
  });

  it('CA-3-11 mientras guarda el botón queda deshabilitado', async () => {
    const { el } = await montar({ guardando: true });
    expect(porId<HTMLButtonElement>(el, 'guardar-tema').disabled).toBe(true);
  });
});
