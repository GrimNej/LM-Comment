# syntax=docker/dockerfile:1.7

FROM node:22.13.1-alpine3.21@sha256:e2b39f7b64281324929257d0f8004fb6cb4bf0fdfb9aa8cedb235a766aec31da AS build

WORKDIR /workspace

RUN npm install --global pnpm@10.34.5 \
    && npm cache clean --force

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY apps/relay/package.json apps/relay/package.json

RUN --mount=type=cache,id=pnpm-store,target=/root/.local/share/pnpm/store \
    pnpm --config.node-linker=isolated install --frozen-lockfile --filter @lm-comment/relay...

COPY apps/relay/tsconfig.json apps/relay/tsconfig.build.json apps/relay/
COPY apps/relay/src apps/relay/src

RUN pnpm --filter @lm-comment/relay build \
    && pnpm --config.node-linker=isolated --config.inject-workspace-packages=true --filter @lm-comment/relay deploy --prod /opt/relay \
    && find /opt/relay/dist -type f \( -name '*.d.ts' -o -name '*.js.map' \) -delete \
    && rm -f /opt/relay/pnpm-lock.yaml \
        /opt/relay/node_modules/.modules.yaml \
        /opt/relay/node_modules/.pnpm/lock.yaml \
        /opt/relay/node_modules/.pnpm-workspace-state-v1.json \
    && node -e "const fs=require('node:fs');const path='/opt/relay/package.json';const manifest=JSON.parse(fs.readFileSync(path));manifest.scripts={start:'node dist/server.js'};delete manifest.devDependencies;delete manifest.files;delete manifest.optionalDependencies;delete manifest.pnpm;fs.writeFileSync(path,JSON.stringify(manifest,null,2)+'\\n')"

FROM node:22.13.1-alpine3.21@sha256:e2b39f7b64281324929257d0f8004fb6cb4bf0fdfb9aa8cedb235a766aec31da AS runtime

ENV NODE_ENV=production \
    HOST=0.0.0.0 \
    PORT=8787

WORKDIR /app

COPY --from=build --chown=node:node /opt/relay/ ./

USER node

EXPOSE 8787
STOPSIGNAL SIGTERM

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD ["node", "-e", "fetch('http://127.0.0.1:' + (process.env.PORT || '8787') + '/healthz').then(response => process.exit(response.ok ? 0 : 1), () => process.exit(1))"]

CMD ["node", "dist/server.js"]
