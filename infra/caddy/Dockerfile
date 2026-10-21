# Imagen del borde (SPEC-8, infraestructura 10.6): Caddy con los dos frontends compilados.
# Contexto de construcción: la raíz del monorepo.
#   docker build -f infra/caddy/Dockerfile -t fitstore/caddy .
# Etapa 1: compilación de los frontends con Node 22 LTS
FROM node:22.23.1-alpine AS frontends
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
RUN npm run build

# Etapa 2: Caddy sirve los estáticos y hace de proxy (el Caddyfile no lleva secretos: todo sale del entorno)
FROM caddy:2.11.3
COPY --from=frontends /src/dist/storefront/browser /srv/tienda
COPY --from=frontends /src/dist/backoffice/browser /srv/admin
COPY infra/caddy/Caddyfile /etc/caddy/Caddyfile
