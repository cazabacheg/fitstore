import { expect, test } from '@playwright/test';
import {
  CLIENTE_PASSWORD,
  TIENDA,
  correoUnico,
  cuerpoCorreo,
  esperarCorreo,
  guardarEstado,
  ingresarCliente,
  registrarCliente,
  tokenCliente,
} from './apoyo';

test.describe('Compra completa (HU-8-02)', () => {
  test('CA-8-06 el visitante se registra, arma su carrito, elige delivery, paga con el simulado y recibe su boleta por correo', async ({
    page,
    request,
  }) => {
    const email = correoUnico('compra');

    // El visitante se registra (la cuenta queda con la sesión iniciada)
    await registrarCliente(page, email);

    // Su dirección de entrega (el alta por diálogo la cubre SPEC-4; aquí se registra por la API)
    const token = await tokenCliente(request, email, CLIENTE_PASSWORD);
    const direccion = await request.post(`${TIENDA}/api/v1/clientes/me/direcciones`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        alias: 'Casa',
        ubigeo: '150115',
        direccion: 'Jr. Gamarra 123, dpto 4',
        referencia: 'Frente al parque',
      },
    });
    expect(direccion.ok()).toBe(true);

    // Arma el carrito: Legging Pro Compresión, con stock
    await page.goto(`${TIENDA}/producto/legging-pro-compresion`);
    // La primera talla con stock (en un entorno ya usado, alguna puede estar agotada)
    await page
      .getByRole('button', { name: /^(S|M|L|XL)$/ })
      .and(page.locator(':enabled'))
      .first()
      .click();
    await page.getByRole('button', { name: 'Agregar al carrito' }).click();
    // Se espera la insignia del carrito y se navega por el enlace (una recarga inmediata aborta el alta en una red lenta)
    await expect(page.getByRole('link', { name: /Carrito/ })).toContainText('1');
    await page.getByRole('link', { name: /Carrito/ }).click();
    await expect(page.getByText('Legging Pro Compresión').first()).toBeVisible();
    await page.getByRole('link', { name: 'Ir al checkout' }).click();
    // Si la sesión no sobrevivió a la recarga, el login devuelve al checkout con returnUrl
    if (/ingresar/.test(page.url())) await ingresarCliente(page, email);

    // Checkout con delivery local
    await expect(page.getByRole('heading', { name: 'Finalizar compra' })).toBeVisible();
    await page.getByRole('radio', { name: /Delivery a domicilio/ }).check();
    await expect(page.getByRole('button', { name: 'Confirmar pedido' })).toBeEnabled();
    await page.getByRole('button', { name: 'Confirmar pedido' }).click();

    // Pago con el proveedor simulado (tarjeta 4111 1111 1111 1111)
    await expect(page).toHaveURL(/\/pago\/FS-\d+/);
    const codigo = /\/pago\/(FS-\d+)/.exec(page.url())?.[1] ?? '';
    await page.getByRole('radio', { name: /prueba|simulad/i }).click();
    await page.getByLabel('Número de tarjeta de prueba').fill('4111 1111 1111 1111');
    await page.getByRole('button', { name: /^Pagar/ }).click();
    await expect(page.getByRole('heading', { name: '¡Pago aprobado!' })).toBeVisible();

    // El correo "¡Pedido confirmado!" con la boleta adjunta llega a Mailpit (verificado por su API)
    const correo = await esperarCorreo(request, email, `Pedido ${codigo}`);
    const detalle = await cuerpoCorreo(request, correo.ID);
    expect(detalle.Text).toContain('¡Pedido confirmado!');
    expect(detalle.Text).toContain(codigo);
    expect(detalle.Attachments.length).toBeGreaterThan(0);

    guardarEstado({ pedido: codigo, clienteEmail: email });
  });
});
