import { expect, test } from '@playwright/test';
import {
  ADMIN,
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  TIENDA,
  correoUnico,
  esperarCorreo,
  ingresarBackoffice,
} from './apoyo';

test.describe('Libro de Reclamaciones (HU-8-02)', () => {
  test('CA-8-10 el visitante registra un reclamo, recibe su número y la copia por correo, y el admin lo responde', async ({
    page,
    request,
  }) => {
    const email = correoUnico('reclamo');

    // El visitante llena la hoja de reclamación
    await page.goto(`${TIENDA}/libro-de-reclamaciones`);
    await page.getByLabel('Nombres').fill('Rosa');
    await page.getByLabel('Apellidos').fill('Quispe Mamani');
    await page.getByLabel('Número de documento').fill('45678912');
    await page.getByLabel('Domicilio').fill('Jr. Gamarra 123, La Victoria');
    await page.getByLabel('Teléfono').fill('987654321');
    await page.getByLabel('Correo electrónico').fill(email);
    await page.getByLabel('Descripción del bien').fill('Legging Pro Compresión talla M');
    await page.getByLabel('Monto reclamado (S/)').fill('59.90');
    await page.getByRole('radio', { name: /Reclamo:/ }).check();
    await page
      .getByLabel('Detalle de lo ocurrido')
      .fill('La costura de la cintura se abrió a la segunda semana de uso.');
    await page
      .getByLabel('Pedido del consumidor: qué solicitas')
      .fill('Quiero el cambio por una prenda nueva.');
    await page.getByRole('button', { name: 'Enviar hoja de reclamación' }).click();

    // Recibe su número y la copia por correo
    const numero = (await page.getByTestId('numero-reclamo').textContent())?.trim() ?? '';
    expect(numero).toMatch(/^RC-/);
    await expect(page.getByTestId('confirmacion')).toContainText(email);
    await esperarCorreo(request, email, numero);

    // El admin lo responde
    await ingresarBackoffice(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto(`${ADMIN}/reclamos?q=${numero}`);
    await page.getByRole('link', { name: numero }).click();
    await page
      .getByTestId('respuesta')
      .fill('Lamentamos lo ocurrido. Cambiaremos la prenda por una nueva sin costo.');
    await page.getByTestId('responder').click();
    await page.getByRole('dialog').getByRole('button', { name: 'Responder reclamo' }).click();
    await expect(page.getByTestId('respuesta-registrada')).toContainText('Cambiaremos la prenda');
  });
});
