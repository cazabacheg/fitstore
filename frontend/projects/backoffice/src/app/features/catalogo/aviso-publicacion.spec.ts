import { montarComp } from './prueba-catalogo';
import { AvisoPublicacion } from './aviso-publicacion';

describe('AvisoPublicacion', () => {
  it('CA-2-24 muestra la lista de lo que falta para publicar', async () => {
    const { el } = await montarComp(AvisoPublicacion, { faltantes: ['Categoría', 'Imagen del color Rojo'] });
    const items = Array.from(el.querySelectorAll('li')).map((li) => li.textContent?.trim());
    expect(items).toEqual(['Categoría', 'Imagen del color Rojo']);
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Falta completar');
  });

  it('CA-2-24 sin faltantes no se muestra', async () => {
    const { el } = await montarComp(AvisoPublicacion, { faltantes: [] });
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });
});
