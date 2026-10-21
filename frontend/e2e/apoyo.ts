import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

/** URLs y credenciales de las E2E (SPEC-8). En CI salen de los datos de demostración de dev. */
export const TIENDA = process.env['E2E_TIENDA'] ?? 'http://localhost:4200';
export const ADMIN = process.env['E2E_ADMIN'] ?? 'http://localhost:4300';
export const MAILPIT = process.env['E2E_MAILPIT'] ?? 'http://localhost:8025';
export const ADMIN_EMAIL = process.env['E2E_ADMIN_EMAIL'] ?? 'admin@fitstore.local';
export const ADMIN_PASSWORD = process.env['E2E_ADMIN_PASSWORD'] ?? 'dev-admin-clave';
export const OPERADOR_EMAIL = process.env['E2E_OPERADOR_EMAIL'] ?? 'diana.flores@fitstore.demo';
export const DEMO_PASSWORD = process.env['E2E_DEMO_PASSWORD'] ?? 'FitStoreDemo2026';
export const CLIENTE_PASSWORD = 'Gamarra2026!x';

/** Datos que una prueba le deja a la siguiente (la compra le entrega su pedido al despacho). */
const ARCHIVO_ESTADO = join(tmpdir(), 'fitstore-e2e-estado.json');
export function guardarEstado(datos: Record<string, string>): void {
  writeFileSync(ARCHIVO_ESTADO, JSON.stringify({ ...leerEstado(), ...datos }));
}
export function leerEstado(): Record<string, string> {
  return existsSync(ARCHIVO_ESTADO) ? JSON.parse(readFileSync(ARCHIVO_ESTADO, 'utf8')) : {};
}

/**
 * El gateway limita los ingresos a 5 por minuto por IP (api-gateway.yml, regla `login`). Todas las pruebas salen de la
 * misma IP, así que antes de cada ingreso se espera si ya hubo 4 en el último minuto (se deja un margen de uno).
 */
const ARCHIVO_INGRESOS = join(tmpdir(), 'fitstore-e2e-ingresos.json');
export async function esperarCupoDeIngreso(): Promise<void> {
  const ahora = Date.now();
  const previos: number[] = existsSync(ARCHIVO_INGRESOS)
    ? JSON.parse(readFileSync(ARCHIVO_INGRESOS, 'utf8'))
    : [];
  const recientes = previos.filter((t) => ahora - t < 61_000).sort((a, b) => a - b);
  let espera = 0;
  if (recientes.length >= 4) espera = recientes[recientes.length - 4] + 61_000 - ahora;
  if (espera > 0) await new Promise((r) => setTimeout(r, espera));
  writeFileSync(ARCHIVO_INGRESOS, JSON.stringify([...recientes, Date.now()]));
}

export const correoUnico = (prefijo: string) => `${prefijo}.${Date.now()}@fitstore.demo`;

interface CorreoMailpit {
  ID: string;
  Subject: string;
  To: { Address: string }[];
  Attachments: number;
}

/** Espera (sondeando la API de Mailpit, I3) un correo para `para` cuyo asunto contenga `asunto`. */
export async function esperarCorreo(
  request: APIRequestContext,
  para: string,
  asunto: string,
): Promise<CorreoMailpit> {
  let encontrado: CorreoMailpit | undefined;
  await expect
    .poll(
      async () => {
        const r = await request.get(`${MAILPIT}/api/v1/search`, {
          params: { query: `to:"${para}"` },
        });
        const cuerpo = (await r.json()) as { messages?: CorreoMailpit[] };
        encontrado = (cuerpo.messages ?? []).find((m) => m.Subject.includes(asunto));
        return encontrado !== undefined;
      },
      {
        message: `correo "${asunto}" para ${para}`,
        timeout: 60_000,
        intervals: [1000, 2000, 3000],
      },
    )
    .toBe(true);
  return encontrado as CorreoMailpit;
}

/** Texto (HTML) de un correo de Mailpit. */
export async function cuerpoCorreo(
  request: APIRequestContext,
  id: string,
): Promise<{ Text: string; HTML: string; Attachments: { FileName: string }[] }> {
  const r = await request.get(`${MAILPIT}/api/v1/message/${id}`);
  return (await r.json()) as { Text: string; HTML: string; Attachments: { FileName: string }[] };
}

/** Ingreso de un cliente por la API (devuelve su token). */
export async function tokenCliente(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<string> {
  await esperarCupoDeIngreso();
  const r = await request.post(`${TIENDA}/api/v1/auth/login`, { data: { email, password } });
  expect(r.ok(), `login de ${email}`).toBe(true);
  return ((await r.json()) as { data: { accessToken: string } }).data.accessToken;
}

/** Registra un cliente nuevo desde la pantalla de registro de la tienda. */
export async function registrarCliente(
  page: Page,
  email: string,
  nombres = 'Eva',
  apellidos = 'Prueba Uno',
): Promise<void> {
  await page.goto(`${TIENDA}/registro`);
  await page.getByLabel('Nombres').fill(nombres);
  await page.getByLabel('Apellidos').fill(apellidos);
  await page.getByLabel('DNI').fill(String(40_000_000 + (Date.now() % 9_000_000)));
  await page.locator('input[type=tel]').fill('987654321');
  await page.getByLabel('Correo').fill(email);
  await page.locator('input[type=password]').fill(CLIENTE_PASSWORD);
  for (const casilla of await page.locator('input[type=checkbox]').all()) await casilla.check();
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(
    page.getByRole('heading', { name: /Muévete|FitStore|Compra por categoría/ }).first(),
  ).toBeVisible();
}

/** Ingreso de un cliente por la pantalla de la tienda. */
export async function ingresarCliente(
  page: Page,
  email: string,
  password = CLIENTE_PASSWORD,
): Promise<void> {
  await esperarCupoDeIngreso();
  await page.getByLabel('Correo').fill(email);
  await page.locator('input[type=password]').fill(password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
}

/** Ingreso al backoffice (ADMIN u OPERADOR). */
export async function ingresarBackoffice(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  await esperarCupoDeIngreso();
  await page.goto(`${ADMIN}/ingresar`);
  await page.getByLabel('Correo').fill(email);
  await page.locator('input[type=password]').fill(password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  await expect(page).not.toHaveURL(/ingresar/);
}
