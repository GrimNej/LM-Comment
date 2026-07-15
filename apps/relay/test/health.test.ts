import { afterEach, describe, expect, it } from 'vitest';

import { buildApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';

const apps: ReturnType<typeof buildApp>[] = [];
afterEach(async () => Promise.all(apps.splice(0).map((app) => app.close())));

describe('GET /healthz', () => {
  it('reports process health without calling Groq', async () => {
    const app = buildApp(loadConfig({ NODE_ENV: 'test' }));
    apps.push(app);
    const response = await app.inject({ method: 'GET', url: '/healthz' });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      status: 'ok',
      generationEnabled: true,
      modelConfigured: true,
    });
  });
});
