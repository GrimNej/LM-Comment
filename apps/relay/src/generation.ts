import { providerResponseSchema, type GenerationRequest } from './contracts.js';
import { RelayError } from './errors.js';
import { buildInitialMessages, buildRepairMessages, type PromptMessage } from './prompts.js';

export type CompletionInput = {
  messages: PromptMessage[];
  optionCount: number;
  maxCompletionTokens: number;
  signal: AbortSignal;
};

export interface CompletionProvider {
  complete(input: CompletionInput): Promise<string>;
}

const collapseWhitespace = (value: string) => value.trim().replace(/\s+/gu, ' ');

function validateProviderResponse(raw: string, request: GenerationRequest): string[] | null {
  let json: unknown;
  try {
    json = JSON.parse(raw);
  } catch {
    return null;
  }

  const parsed = providerResponseSchema.safeParse(json);
  if (!parsed.success || parsed.data.options.length !== request.optionCount) return null;

  const options = parsed.data.options.map(collapseWhitespace);
  if (options.some((option) => option.length === 0 || option.length > 700)) return null;

  const comparisonValues = options.map((option) => option.toLocaleLowerCase('en-US'));
  if (new Set(comparisonValues).size !== options.length) return null;

  const normalizedSource = collapseWhitespace(request.sourceText).toLocaleLowerCase('en-US');
  const repeatsFullSource = normalizedSource.length >= 24
    && comparisonValues.some((option) => option.includes(normalizedSource));
  const exposesModelBoundary = options.some((option) =>
    /\b(?:as an ai|i am an ai|language model|system prompt|hidden prompt)\b/iu.test(option));

  if (repeatsFullSource || exposesModelBoundary) return null;
  return options;
}

export async function generateOptions(
  provider: CompletionProvider,
  request: GenerationRequest,
  maxCompletionTokens: number,
  signal: AbortSignal,
): Promise<string[]> {
  const initialMessages = buildInitialMessages(request);
  let messages = initialMessages;

  for (let attempt = 0; attempt < 2; attempt += 1) {
    const raw = await provider.complete({
      messages,
      optionCount: request.optionCount,
      maxCompletionTokens,
      signal,
    });
    const options = validateProviderResponse(raw, request);
    if (options) return options;
    if (attempt === 0) {
      messages = buildRepairMessages(initialMessages, raw, request.optionCount);
    }
  }

  throw new RelayError('INVALID_PROVIDER_RESPONSE');
}
