# Build static files
# Node Bullseye has npm
FROM node:22.17.0-bullseye-slim AS build
ARG CONTEXT="./frontend"
# Install packages, build and keep only prod packages
WORKDIR /app
COPY ${CONTEXT}/*.json ${CONTEXT}/*.ts ${CONTEXT}/index.html ./
COPY ${CONTEXT}/public ./public
COPY ${CONTEXT}/src ./src
RUN npm ci --ignore-scripts --no-update-notifier && \
    npm run build-only

# Deploy using Caddy to host static files
FROM caddy:2.8.4-alpine

# Install packages and ensure permissions in one RUN command
RUN apk add --no-cache ca-certificates && \
    mkdir -p /config/caddy /data/caddy && \
    chown -R 1001:1001 /config/caddy /data/caddy

# Copy the root CA certificate to /etc/ssl/certs
ARG CONTEXT="./frontend"
COPY ${CONTEXT}/entrust_g2_ca.crt /etc/ssl/certs/entrust_g2_ca.crt
RUN update-ca-certificates

# Copy static files and run formatting
ARG CONTEXT="./frontend"
COPY --from=build /app/dist /srv
COPY ${CONTEXT}/Caddyfile /etc/caddy/Caddyfile
RUN caddy fmt --overwrite /etc/caddy/Caddyfile

RUN chmod 755 /usr/bin/caddy

# Ports, health check and non-root user
EXPOSE 3000 3001
HEALTHCHECK --interval=30s --timeout=3s CMD ["curl", "-f", "http://localhost:3001/health"]
USER 1001
