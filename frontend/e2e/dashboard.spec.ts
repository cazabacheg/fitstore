import { expect, test } from '@playwright/test';
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  DEMO_PASSWORD,
  OPERADOR_EMAIL,
  ingresarBackoffice,
} from './apoyo';

test.describe('Dashboard del backoffice (HU-8-01)', () => {
  test('CA-8-01 el ADMIN ve pedidos por estado, stock bajo, entregas de hoy y reclamos', async ({
    page,
  }) => {
    await ingresarBackoffice(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    for (const titulo of ['Pedidos por estado', 'Stock bajo', 'Entregas de hoy', 'Reclamos']) {
      await expect(page.getByRole('heading', { name: titulo })).toBeVisible();
    }
    // Con los datos de demostración hay pedidos pagados y entregas
    await expect(page.getByRole('link', { name: /Pagado/ })).toContainText(/\d+/);
  });

  test('CA-8-02 el OPERADOR ve lo mismo (sin ventas ni top de productos)', async ({ page }) => {
    await ingresarBackoffice(page, OPERADOR_EMAIL, DEMO_PASSWORD);
    await expect(page.getByRole('heading', { name: 'Pedidos por estado' })).toBeVisible();
    await expect(page.getByText(/Ventas|Más vendidos/)).toHaveCount(0);
  });

  test('CA-8-03 cada tarjeta lleva a su pantalla con el filtro aplicado', async ({ page }) => {
    await ingresarBackoffice(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.getByRole('link', { name: /Pagado/ }).click();
    await expect(page).toHaveURL(/\/pedidos\?estado=PAGADO/);
    await page.getByRole('link', { name: 'Dashboard' }).click();
    await page.getByRole('link', { name: 'Por vencer' }).click();
    await expect(page).toHaveURL(/\/reclamos\?plazo=porVencer/);
    await page.getByRole('link', { name: 'Dashboard' }).click();
    await page.getByRole('link', { name: 'Entregas de hoy' }).click();
    await expect(page).toHaveURL(/\/entregas/);
  });

  test('CA-8-04 y CA-8-05 si delivery no responde solo su tarjeta dice "No disponible"; "Actualizar" y "Reintentar" vuelven a pedir', async ({
    page,
  }) => {
    let caido = true;
    await page.route('**/api/v1/admin/entregas*', (ruta) =>
      caido ? ruta.fulfill({ status: 503, body: '{}' }) : ruta.continue(),
    );
    await ingresarBackoffice(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await expect(page.getByText('No disponible')).toHaveCount(1);
    await expect(page.getByRole('heading', { name: 'Pedidos por estado' })).toBeVisible();
    await expect(page.getByRole('link', { name: /Pagado/ })).toBeVisible();
    await expect(page.getByText(/Actualizado a las \d{2}:\d{2}/)).toBeVisible();

    caido = false;
    await page.getByRole('button', { name: 'Reintentar' }).click();
    await expect(page.getByText('No disponible')).toHaveCount(0);
    await expect(page.getByText('Programadas')).toBeVisible();

    // Sin actualización automática: solo "Actualizar" pide de nuevo
    let pedidos = 0;
    await page.route('**/api/v1/admin/pedidos*', (ruta) => {
      pedidos++;
      return ruta.continue();
    });
    await page.getByRole('button', { name: 'Actualizar' }).click();
    await expect.poll(() => pedidos).toBe(1);
  });
});
