import Fastify, { type FastifyInstance } from 'fastify';

import type { RelayConfig } from './config.js';

export function buildApp(config: RelayConfig): FastifyInstance {
  const app = Fastify({
    bodyLimit: 32 * 1024,
    logger: config.NODE_ENV === 'test'
      ? false
      : {
          level: config.NODE_ENV === 'production' ? 'info' : 'debug',
          redact: {
            paths: [
              'req.headers.x-demo-token',
              'req.headers.authorization',
              'request.body',
              'response.body',
            ],
            censor: '[REDACTED]',
          },
        },
    disableRequestLogging: true,
    requestIdHeader: 'x-request-id',
  });

  app.get('/healthz', async () => ({
    status: 'ok',
    generationEnabled: config.GENERATION_ENABLED,
    modelConfigured: Boolean(config.GROQ_MODEL),
  }));

  return app;
}
