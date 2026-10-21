// CA-2-55: 50 usuarios virtuales sobre el listado y el detalle del catálogo; el p95 debe ser menor a 500 ms (RNF-01).
//
// Se corre en Lyanna con la plataforma levantada (docker compose) y datos de demostración (perfil dev):
//   k6 run -e BASE_URL=http://localhost:8080 -e CACHE=con k6/catalogo.js
//   docker compose stop redis && k6 run -e BASE_URL=http://localhost:8080 -e CACHE=sin k6/catalogo.js; docker compose start redis
// o con Docker:  docker run --rm -i --network host -e BASE_URL=http://localhost:8080 grafana/k6 run - < k6/catalogo.js
//
// La segunda corrida mide "sin caché" (Redis detenido: el catálogo lee de la base, CA-2-57) para comparar en la exposición.
// Las dos corridas usan la misma etiqueta `cache` en las métricas para separarlas en el resumen.
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CACHE = __ENV.CACHE || 'con';

export const options = {
  vus: 50,
  duration: __ENV.DURACION || '1m',
  tags: { cache: CACHE },
  thresholds: {
    // CA-2-55
    http_req_duration: ['p(95)<500'],
    'http_req_duration{grupo:listado}': ['p(95)<500'],
    'http_req_duration{grupo:detalle}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const FILTROS = [
  '',
  '?categoria=leggings',
  '?categoria=polos-y-tops',
  '?talla=M&color=1',
  '?q=legging',
  '?sort=precio,asc',
  '?disponible=true&size=12',
];

// Antes de cargar: toma de la tienda los slugs reales de los productos
export function setup() {
  const respuesta = http.get(`${BASE_URL}/api/v1/productos?size=48`);
  check(respuesta, { 'el catálogo responde 200': (r) => r.status === 200 });
  const slugs = respuesta.json('data.contenido').map((p) => p.slug);
  if (slugs.length === 0) {
    throw new Error('El catálogo está vacío: levanta la plataforma con el perfil dev (datos de demostración)');
  }
  return { slugs };
}

export default function (datos) {
  group('listado', () => {
    const filtro = FILTROS[Math.floor(Math.random() * FILTROS.length)];
    const r = http.get(`${BASE_URL}/api/v1/productos${filtro}`, { tags: { grupo: 'listado' } });
    check(r, { 'listado 200': (x) => x.status === 200 });
  });
  group('detalle', () => {
    const slug = datos.slugs[Math.floor(Math.random() * datos.slugs.length)];
    const r = http.get(`${BASE_URL}/api/v1/productos/${slug}`, { tags: { grupo: 'detalle' } });
    check(r, { 'detalle 200': (x) => x.status === 200, 'trae variantes': (x) => x.json('data.variantes').length > 0 });
  });
  sleep(Math.random() * 0.5);
}
