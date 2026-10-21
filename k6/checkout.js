// CA-4-26: el checkout completo responde con p95 < 2 s con 20 usuarios simultáneos y las dependencias reales en compose.
//
// Recorrido de cada usuario virtual: inicia sesión con un cliente de demostración, arma su carrito (CA-4-01),
// pide el resumen (CA-4-16) y confirma el pedido con su Idempotency-Key (CA-4-19). El umbral de 2 s se exige al
// recorrido completo del checkout (resumen + confirmación), que es lo que mide RNF-02.
//
// Uso (plataforma levantada con los datos de demostración):
//   k6 run k6/checkout.js
//   k6 run -e BASE_URL=https://tienda.example.org -e PASSWORD=... k6/checkout.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.PASSWORD || 'FitStoreDemo2026';
// Variantes de la demo de catalog (R__catalogo_demo.sql) con stock de sobra
const VARIANTES = (__ENV.VARIANTES || '10009,10021,10040').split(',').map(Number);
const CLIENTES = [
  'lucia.mendoza@fitstore.demo',
  'carlos.huaman@fitstore.demo',
  'rosa.ccori@fitstore.demo',
  'miguel.paredes@fitstore.demo',
];

const duracionCheckout = new Trend('checkout_completo', true);

export const options = {
  scenarios: {
    checkout: { executor: 'constant-vus', vus: 20, duration: '1m' },
  },
  thresholds: {
    // CA-4-26
    checkout_completo: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function conSesion(token, extra = {}) {
  return { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}`, ...extra } };
}

export default function () {
  const email = CLIENTES[(__VU - 1) % CLIENTES.length];
  const login = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({ email, password: PASSWORD }), {
    headers: JSON_HEADERS,
  });
  if (!check(login, { 'inicia sesión': (r) => r.status === 200 })) {
    return;
  }
  const token = login.json('data.accessToken');

  const varianteId = VARIANTES[(__VU + __ITER) % VARIANTES.length];
  http.del(`${BASE_URL}/api/v1/carrito`, null, conSesion(token));
  const agregado = http.post(`${BASE_URL}/api/v1/carrito/items`, JSON.stringify({ varianteId, cantidad: 1 }),
    conSesion(token));
  check(agregado, { 'agrega al carrito': (r) => r.status === 200 });

  // Recojo en tienda: no depende de las direcciones ni de las tarifas de cada cliente
  const inicio = Date.now();
  const resumen = http.post(`${BASE_URL}/api/v1/checkout/resumen`, JSON.stringify({ modalidad: 'RECOJO' }),
    conSesion(token));
  const resumenOk = check(resumen, { 'resumen 200': (r) => r.status === 200 });
  if (resumenOk) {
    const confirmacion = http.post(`${BASE_URL}/api/v1/pedidos`,
      JSON.stringify({ modalidad: 'RECOJO', totalEsperado: resumen.json('data.total') }),
      conSesion(token, { 'Idempotency-Key': uuidv4() }));
    check(confirmacion, {
      'pedido creado': (r) => r.status === 201,
      'devuelve el código FS-…': (r) => /^FS-\d{6}$/.test(r.json('data.codigo')),
    });
  }
  duracionCheckout.add(Date.now() - inicio);
  sleep(1);
}
