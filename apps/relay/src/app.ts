import { createHash, randomUUID, timingSafeEqual } from 'node:crypto';
import { performance } from 'node:perf_hooks';

import rateLimit from '@fastify/rate-limit';
import Fastify, {
  LogController,
  type FastifyInstance,
  type FastifyRequest,
} from 'fastify';

import type { RelayConfig } from './config.js';
import {
  generationRequestSchema,
  requestJsonSchema,
  responseJsonSchema,
  type GenerationRequest,
  type GenerationResponse,
} from './contracts.js';
import {
  errorBody,
  isBadRequestError,
  RelayError,
  type RelayErrorCode,
} from './errors.js';
import { generateOptions, type CompletionProvider } from './generation.js';
import { GroqCompletionProvider } from './groq-provider.js';
import { DailyRequestCounter } from './rate-limit.js';

const OVERALL_RELAY_TIMEOUT_MS = 20_000;
const HANDLER_TIMEOUT_GRACE_MS = 5_000;

type LogStream = { write(chunk: string): void };

export type BuildAppDependencies = {
  provider?: CompletionProvider;
  now?: () => Date;
  overallTimeoutMs?: number;
  logStream?: LogStream;
};

type RequestMetadata = {
  startedAt: number;
  currentCount: number;
  errorCode: RelayErrorCode | null;
};

function tokenMatches(actual: string | undefined, expected: string | undefined): boolean {
  if (!actual || !expected) return false;
  const actualDigest = createHash('sha256').update(actual, 'utf8').digest();
  const expectedDigest = createHash('sha256').update(expected, 'utf8').digest();
  return timingSafeEqual(actualDigest, expectedDigest);
}

function demoToken(request: FastifyRequest): string | undefined {
  const value = request.headers['x-demo-token'];
  return Array.isArray(value) ? value[0] : value;
}

function rateLimitKey(request: FastifyRequest): string {
  const tokenDigest = createHash('sha256')
    .update(demoToken(request) ?? '', 'utf8')
    .digest('hex')
    .slice(0, 32);
  return `${request.ip}:${tokenDigest}`;
}

function withOverallTimeout<T>(
  operation: (signal: AbortSignal) => Promise<T>,
  timeoutMs: number,
): Promise<T> {
  const controller = new AbortController();
  let timer: NodeJS.Timeout | undefined;
  const timeout = new Promise<never>((_resolve, reject) => {
    timer = setTimeout(() => {
      controller.abort();
      reject(new RelayError('PROVIDER_TIMEOUT'));
    }, timeoutMs);
  });

  return Promise.race([operation(controller.signal), timeout])
    .finally(() => {
      if (timer) clearTimeout(timer);
    });
}

export function buildApp(
  config: RelayConfig,
  dependencies: BuildAppDependencies = {},
): FastifyInstance {
  const providerTimeoutMs = dependencies.overallTimeoutMs ?? OVERALL_RELAY_TIMEOUT_MS;
  const logger = config.NODE_ENV === 'test' && !dependencies.logStream
    ? false
    : {
        level: config.NODE_ENV === 'production' ? 'info' : 'debug',
        ...(dependencies.logStream ? { stream: dependencies.logStream } : {}),
        redact: {
          paths: [
            'req.headers.x-demo-token',
            'req.headers.authorization',
            'request.body',
            'response.body',
            '*.sourceText',
            '*.options',
            '*.GROQ_API_KEY',
          ],
          censor: '[REDACTED]',
        },
      };

  const app = Fastify({
    ajv: { customOptions: { removeAdditional: false } },
    bodyLimit: 32 * 1024,
    // Keep Fastify's outer timeout later than the provider deadline so the
    // inner timer can abort the provider and return the stable public error.
    handlerTimeout: providerTimeoutMs + HANDLER_TIMEOUT_GRACE_MS,
    logger,
    logController: new LogController({ disableRequestLogging: true }),
    requestIdHeader: false,
    genReqId: () => randomUUID(),
    exposeHeadRoutes: false,
    // Production traffic reaches this process only through the private
    // Docker bridge used by the host's Caddy container. Trust RFC1918 proxy
    // addresses so rate limits use the real client IP forwarded by Caddy.
    trustProxy: config.NODE_ENV === 'production' ? 'uniquelocal' : false,
  });

  const metadata = new WeakMap<FastifyRequest, RequestMetadata>();
  const dailyCounter = new DailyRequestCounter(dependencies.now);
  const provider = dependencies.provider
    ?? (config.GROQ_API_KEY
      ? new GroqCompletionProvider(config.GROQ_API_KEY, config.GROQ_MODEL)
      : undefined);

  void app.register(rateLimit, {
    global: false,
    hook: 'preHandler',
    max: config.MAX_REQUESTS_PER_MINUTE,
    timeWindow: 60_000,
    keyGenerator: rateLimitKey,
    errorResponseBuilder: (request) => {
      const requestMetadata = metadata.get(request);
      if (requestMetadata) requestMetadata.errorCode = 'RATE_LIMITED';
      return new RelayError('RATE_LIMITED');
    },
  });

  app.addHook('onResponse', async (request, reply) => {
    const requestMetadata = metadata.get(request);
    if (!requestMetadata) return;
    request.log.info({
      requestId: request.id,
      route: '/v1/generate',
      status: reply.statusCode,
      latencyMs: Math.round((performance.now() - requestMetadata.startedAt) * 10) / 10,
      errorCode: requestMetadata.errorCode,
      currentCount: requestMetadata.currentCount,
    }, 'relay request completed');
  });

  app.setErrorHandler((error, request, reply) => {
    const relayError = error instanceof RelayError
      ? error
      : isBadRequestError(error)
        ? new RelayError('BAD_REQUEST')
        : new RelayError('INTERNAL');
    const requestMetadata = metadata.get(request);
    if (requestMetadata) requestMetadata.errorCode = relayError.code;
    void reply.code(relayError.statusCode).send(errorBody(relayError, request.id));
  });

  app.get('/healthz', async () => ({
    status: 'ok',
    generationEnabled: config.GENERATION_ENABLED,
    modelConfigured: Boolean(config.GROQ_MODEL),
  }));

  app.after(() => {
    app.post<{ Body: GenerationRequest; Reply: GenerationResponse }>('/v1/generate', {
    schema: {
      body: requestJsonSchema(config.MAX_SOURCE_CHARACTERS),
      response: { 200: responseJsonSchema },
    },
    config: {
      rateLimit: {
        max: config.MAX_REQUESTS_PER_MINUTE,
        timeWindow: 60_000,
        keyGenerator: rateLimitKey,
      },
    },
    onRequest: [
      async (request) => {
        metadata.set(request, {
          startedAt: performance.now(),
          currentCount: 0,
          errorCode: null,
        });
      },
      async (request) => {
        if (!tokenMatches(demoToken(request), config.DEMO_TOKEN)) {
          throw new RelayError('UNAUTHORIZED');
        }
        if (!config.GENERATION_ENABLED) {
          throw new RelayError('GENERATION_DISABLED');
        }
      },
    ],
  }, async (request) => {
    if (!provider) throw new RelayError('PROVIDER_UNAVAILABLE');

    const parsedRequest = generationRequestSchema.safeParse(request.body);
    if (!parsedRequest.success
      || parsedRequest.data.sourceText.length > config.MAX_SOURCE_CHARACTERS) {
      throw new RelayError('BAD_REQUEST');
    }

    const daily = dailyCounter.tryIncrement(config.MAX_DAILY_REQUESTS);
    const requestMetadata = metadata.get(request);
    if (requestMetadata) requestMetadata.currentCount = daily.currentCount;
    if (!daily.allowed) throw new RelayError('DAILY_LIMIT_REACHED');

    const options = await withOverallTimeout(
      (signal) => generateOptions(
        provider,
        parsedRequest.data,
        config.MAX_COMPLETION_TOKENS,
        signal,
      ),
      providerTimeoutMs,
    );

    return {
      requestId: request.id,
      options: options.map((text) => ({ id: randomUUID(), text })),
    };
    });
  });

  return app;
}
