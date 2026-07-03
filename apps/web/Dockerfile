# syntax=docker/dockerfile:1

###############################################################################
# Stage 1 — build Angular app
###############################################################################
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json ./
RUN npm install --legacy-peer-deps

COPY . .
RUN npm run build

###############################################################################
# Stage 2 — serve with nginx (SPA fallback + /v1 proxy to api)
###############################################################################
FROM nginx:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/web/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
