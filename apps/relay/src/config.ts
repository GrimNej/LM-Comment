import { z } from 'zod';

const booleanFromString = z.preprocess(
  (value) => value === undefined ? undefined : value === true || value === 'true',
  z.boolean(),
);

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().min(1).max(65_535).default(3000),
  HOST: z.string().default('0.0.0.0'),
  GROQ_API_KEY: z.string().min(1).optional(),
  DEMO_TOKEN: z.string().min(12).optional(),
  GROQ_MODEL: z.string().min(1).default('openai/gpt-oss-120b'),
  MAX_REQUESTS_PER_MINUTE: z.coerce.number().int().min(1).max(60).default(3),
  MAX_DAILY_REQUESTS: z.coerce.number().int().min(1).max(10_000).default(100),
  MAX_SOURCE_CHARACTERS: z.coerce.number().int().min(1).max(8_000).default(8_000),
  MAX_COMPLETION_TOKENS: z.coerce.number().int().min(64).max(2_048).default(700),
  GENERATION_ENABLED: booleanFromString.default(true),
});

export type RelayConfig = z.infer<typeof envSchema>;

export function loadConfig(source: NodeJS.ProcessEnv = process.env): RelayConfig {
  return envSchema.parse(source);
}
