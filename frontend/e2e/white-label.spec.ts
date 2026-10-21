import { expect, test, type Page } from '@playwright/test';
import { ADMIN, ADMIN_EMAIL, ADMIN_PASSWORD, TIENDA, ingresarBackoffice } from './apoyo';

const NOMBRE_NUEVO = 'Kallpa Wear';
const COLOR_NUEVO = '#0B6E4F';

/** Cambia el nombre de la tienda (pestaña Identidad) y el color primario (pestaña Tema). */
async function cambiarMarca(page: Page, nombre: string, colorPrimario: string): Promise<void> {
  await page.getByTestId('pestana-identidad').click();
  await page.getByTestId('nombre').fill(nombre);
  await page.getByRole('button', { name: 'Guardar identidad' }).click();
  await expect(page.getByTestId('nombre')).toHaveValue(nombre);

  await page.getByTestId('pestana-tema').click();
  await page.getByTestId('color-primario').fill(colorPrimario);
  await page.getByTestId('guardar-tema').click();
  await expect(page.getByTestId('color-primario')).toHaveValue(colorPrimario);
  // Espera a que el guardado termine (el botón vuelve a "Guardar tema")
  await expect(page.getByTestId('guardar-tema')).toHaveText('Guardar tema');
}

test.describe('White-label (HU-8-02)', () => {
  test('CA-8-09 el admin cambia el color primario y el nombre de la tienda y, al recargar la tienda, se ven los cambios', async ({
    page,
    browser,
  }) => {
    await ingresarBackoffice(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto(`${ADMIN}/configuracion`);
    await page.getByTestId('pestana-identidad').click();
    const nombreOriginal = await page.getByTestId('nombre').inputValue();
    await page.getByTestId('pestana-tema').click();
    const colorOriginal = await page.getByTestId('color-primario').inputValue();

    try {
      await cambiarMarca(page, NOMBRE_NUEVO, COLOR_NUEVO);

      // La tienda, en otro navegador (sin sesión), muestra la marca nueva al cargarla
      await expect
        .poll(
          async () =>
            (await (await page.request.get(`${TIENDA}/api/v1/settings/public`)).json()).data.tema
              .colorPrimario,
        )
        .toBe(COLOR_NUEVO);
      const visitante = await browser.newContext();
      const tienda = await visitante.newPage();
      await tienda.goto(`${TIENDA}/`);
      await expect(tienda.locator('header').first()).toContainText(NOMBRE_NUEVO, {
        ignoreCase: true,
      });
      await expect
        .poll(() =>
          tienda.evaluate(() =>
            getComputedStyle(document.documentElement)
              .getPropertyValue('--fs-color-primario')
              .trim()
              .toLowerCase(),
          ),
        )
        .toBe(COLOR_NUEVO.toLowerCase());
      await visitante.close();
    } finally {
      // Deja la tienda como estaba para las demás pruebas
      await page.goto(`${ADMIN}/configuracion`);
      await cambiarMarca(page, nombreOriginal, colorOriginal);
    }
  });
});
