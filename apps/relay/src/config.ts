import { z } from 'zod';

export const FIXED_GROQ_MODEL = 'openai/gpt-oss-120b' as const;

const booleanFromString = z.union([z.boolean(), z.enum(['true', 'false'])])
  .transform((value) => value === true || value === 'true');

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().min(1).max(65_535).default(3000),
  HOST: z.string().default('0.0.0.0'),
  GROQ_API_KEY: z.string().min(1).optional(),
  DEMO_TOKEN: z.string().min(12).optional(),
  GROQ_MODEL: z.literal(FIXED_GROQ_MODEL).default(FIXED_GROQ_MODEL),
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

export function assertRunnableConfig(config: RelayConfig): void {
  if (!config.GENERATION_ENABLED) return;

  const missing = [
    !config.GROQ_API_KEY && 'GROQ_API_KEY',
    !config.DEMO_TOKEN && 'DEMO_TOKEN',
  ].filter(Boolean);

  if (missing.length > 0) {
    throw new Error(`Missing relay configuration: ${missing.join(', ')}`);
  }
}
