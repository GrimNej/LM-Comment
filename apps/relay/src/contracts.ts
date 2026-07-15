import { z } from 'zod';

export const toneValues = [
  'natural',
  'professional',
  'friendly',
  'witty',
  'concise',
] as const;

export const generationRequestSchema = z.object({
  sourceText: z.string().min(1).max(8_000),
  tone: z.enum(toneValues),
  instruction: z.string().max(500),
  optionCount: z.number().int().min(1).max(3),
}).strict();

export const providerResponseSchema = z.object({
  options: z.array(z.string().min(1).max(700)).min(1).max(3),
}).strict();

export const generationResponseSchema = z.object({
  requestId: z.uuid(),
  options: z.array(z.object({
    id: z.uuid(),
    text: z.string().min(1).max(700),
  }).strict()).min(1).max(3),
}).strict();

export type GenerationRequest = z.infer<typeof generationRequestSchema>;
export type GenerationResponse = z.infer<typeof generationResponseSchema>;

export function requestJsonSchema(maxSourceCharacters: number) {
  return {
    type: 'object',
    additionalProperties: false,
    required: ['sourceText', 'tone', 'instruction', 'optionCount'],
    properties: {
      sourceText: { type: 'string', minLength: 1, maxLength: maxSourceCharacters },
      tone: { type: 'string', enum: toneValues },
      instruction: { type: 'string', maxLength: 500 },
      optionCount: { type: 'integer', minimum: 1, maximum: 3 },
    },
  } as const;
}

export const responseJsonSchema = {
  type: 'object',
  additionalProperties: false,
  required: ['requestId', 'options'],
  properties: {
    requestId: { type: 'string', format: 'uuid' },
    options: {
      type: 'array',
      minItems: 1,
      maxItems: 3,
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['id', 'text'],
        properties: {
          id: { type: 'string', format: 'uuid' },
          text: { type: 'string', minLength: 1, maxLength: 700 },
        },
      },
    },
  },
} as const;

export function providerJsonSchema(optionCount: number) {
  return {
    type: 'object',
    additionalProperties: false,
    required: ['options'],
    properties: {
      options: {
        type: 'array',
        minItems: optionCount,
        maxItems: optionCount,
        items: { type: 'string', minLength: 1, maxLength: 700 },
      },
    },
  } as const;
}
