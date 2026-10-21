import { expect, test, type Page } from '@playwright/test';
import {
  ADMIN,
  CLIENTE_PASSWORD,
  DEMO_PASSWORD,
  OPERADOR_EMAIL,
  TIENDA,
  esperarCorreo,
  ingresarBackoffice,
  ingresarCliente,
  leerEstado,
} from './apoyo';

/** Pulsa un botón de la pantalla y, si abre un diálogo, confirma con el botón del mismo nombre. */
async function accionConDialogo(page: Page, nombre: string | RegExp): Promise<void> {
  await page.getByRole('button', { name: nombre }).first().click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo).toBeVisible();
  await dialogo.getByRole('button', { name: nombre }).click();
  await expect(dialogo).toBeHidden();
}

test.describe('Despacho y entrega (HU-8-02)', () => {
  test('CA-8-07 el operador despacha el pedido hasta entregarlo y el cliente ve "Entregado" y recibe los correos', async ({
    page,
    request,
  }) => {
    const { pedido, clienteEmail } = leerEstado();
    expect(pedido, 'la compra (compra.spec.ts) deja el pedido a despachar').toBeTruthy();

    // El operador prepara el pedido y lo deja listo para despacho
    await ingresarBackoffice(page, OPERADOR_EMAIL, DEMO_PASSWORD);
    await page.goto(`${ADMIN}/pedidos?q=${pedido}`);
    await page.getByRole('link', { name: pedido }).click();
    await expect(page.getByRole('heading', { name: `Pedido ${pedido}` })).toBeVisible();
    await accionConDialogo(page, 'Empezar a preparar');
    await accionConDialogo(page, /Marcar listo para despacho/);
    await expect(page.getByRole('heading', { name: 'Historial' })).toBeVisible();
    await expect(page.getByText('Listo para despacho', { exact: true }).first()).toBeVisible();

    // Asigna repartidor, marca en ruta y entregada
    // (se navega por el menú, sin recargar: una recarga rápida y repetida compite con el refresco de la sesión)
    await page.getByRole('button', { name: /Entregas/ }).click();
    await page.getByRole('link', { name: 'Lista' }).click();
    const buscador = page.getByRole('searchbox');
    const fila = page.getByRole('row').filter({ hasText: pedido });
    // La entrega la crea delivery al consumir el evento del pedido: se vuelve a buscar hasta que aparece
    await expect(async () => {
      await buscador.fill('');
      await page.waitForTimeout(800); // el buscador espera un momento antes de consultar
      await buscador.fill(pedido);
      await expect(fila).toBeVisible({ timeout: 4000 });
    }).toPass({ timeout: 45_000, intervals: [1500] });
    await fila.getByRole('button', { name: /Asignar/ }).click();
    const dialogo = page.getByRole('dialog');
    await dialogo.getByLabel('Repartidor').selectOption({ index: 1 });
    await dialogo.getByRole('button', { name: 'Guardar' }).click();
    await expect(dialogo).toBeHidden();

    await fila.getByRole('button', { name: /En ruta/ }).click();
    await expect(fila.getByText('En ruta')).toBeVisible();

    await fila.getByRole('button', { name: /Entregada/ }).click();
    await expect(fila.getByText('Entregada')).toBeVisible();

    // Los correos "en camino" y "entregado" llegan a Mailpit
    await esperarCorreo(request, clienteEmail, 'camino');
    await esperarCorreo(request, clienteEmail, 'entregado');

    // El cliente ve "Entregado" en la línea de tiempo de su pedido
    await page.context().clearCookies();
    await page.goto(
      `${TIENDA}/ingresar?returnUrl=${encodeURIComponent('/cuenta/pedidos/' + pedido)}`,
    );
    await ingresarCliente(page, clienteEmail, CLIENTE_PASSWORD);
    await expect(page.getByText('Entregado').first()).toBeVisible();
  });
});
