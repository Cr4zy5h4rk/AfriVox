# Frontend Angular servi par nginx. Contexte : le depot Afrivox_Frontend (voir deploy/docker-compose.yml).

# ---- Etape 1 : build de production ---------------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ---- Etape 2 : nginx -----------------------------------------------------------------------------------------------
FROM nginx:1.27-alpine
# La configuration (modele avec ${DOMAIN}) est montee par docker-compose dans /etc/nginx/templates.
COPY --from=build /app/dist/afrivox-web/browser /usr/share/nginx/html
