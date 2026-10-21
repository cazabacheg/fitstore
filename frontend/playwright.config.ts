import { defineConfig, devices } from '@playwright/test';

/**
 * Pruebas E2E de SPEC-8 (CA-8-06 a CA-8-11): navegador real contra el compose completo, con el proveedor de pago
 * simulado y la API de Mailpit para los correos (I3). Las URLs salen del entorno para correr igual en local, en
 * stage.yml (Caddy.local en 4200 y 4300) y contra la nube.
 *
 *   E2E_TIENDA   storefront        (por defecto http://localhost:4200)
 *   E2E_ADMIN    backoffice        (por defecto http://localhost:4300)
 *   E2E_MAILPIT  API de Mailpit    (por defecto http://localhost:8025)
 */
export default defineConfig({
  testDir: './e2e',
  // Las pruebas comparten el mismo sistema (pedidos, marca, stock): una a la vez y en orden de archivo
  fullyParallel: false,
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : [['list']],
  // CA-8-11: capturas y video de cada fallo, que stage.yml sube como artefacto
  outputDir: 'e2e-resultados',
  use: {
    baseURL: process.env['E2E_TIENDA'] ?? 'http://localhost:4200',
    locale: 'es-PE',
    timezoneId: 'America/Lima',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
