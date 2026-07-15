import { describe, expect, it } from 'vitest';

import { assertRunnableConfig, FIXED_GROQ_MODEL, loadConfig } from '../src/config.js';

describe('relay configuration', () => {
  it('pins the single supported production model', () => {
    expect(loadConfig({ NODE_ENV: 'test' }).GROQ_MODEL).toBe(FIXED_GROQ_MODEL);
    expect(() => loadConfig({ NODE_ENV: 'test', GROQ_MODEL: 'llama-3.3-70b-versatile' })).toThrow();
  });

  it('requires host secrets when generation is enabled at server startup', () => {
    expect(() => assertRunnableConfig(loadConfig({ NODE_ENV: 'production' }))).toThrow(
      /GROQ_API_KEY, DEMO_TOKEN/,
    );
    expect(() => assertRunnableConfig(loadConfig({
      NODE_ENV: 'production',
      GENERATION_ENABLED: 'false',
    }))).not.toThrow();
  });

  it('rejects ambiguous boolean configuration', () => {
    expect(() => loadConfig({ NODE_ENV: 'test', GENERATION_ENABLED: 'yes' })).toThrow();
  });
});
