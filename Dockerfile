# Coolify Full-Stack Backend + APK Distribution Server Dockerfile
FROM node:20-alpine

WORKDIR /srv/vidyakosha

# Install dependencies for Coolify Backend API + PostgreSQL client
COPY coolify-backend/package.json ./coolify-backend/package.json
RUN cd coolify-backend && npm install --omit=dev

# Copy backend code, SQL migrations, web portal, and released Android APK
COPY coolify-backend/ ./coolify-backend/
COPY coolify/ ./coolify/
COPY releases/ ./releases/

ENV PORT=80
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:80/healthz || exit 1

CMD ["node", "coolify-backend/server.js"]
