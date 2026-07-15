import Groq, {
  APIConnectionError,
  APIConnectionTimeoutError,
  APIError,
  APIUserAbortError,
  RateLimitError,
} from 'groq-sdk';

import { providerJsonSchema } from './contracts.js';
import { RelayError } from './errors.js';
import type { CompletionInput, CompletionProvider } from './generation.js';

const GROQ_REQUEST_TIMEOUT_MS = 15_000;

export class GroqCompletionProvider implements CompletionProvider {
  private readonly client: Groq;

  constructor(apiKey: string, private readonly model: string) {
    this.client = new Groq({
      apiKey,
      timeout: GROQ_REQUEST_TIMEOUT_MS,
      maxRetries: 0,
    });
  }

  async complete(input: CompletionInput): Promise<string> {
    try {
      const completion = await this.client.chat.completions.create({
        model: this.model,
        messages: input.messages,
        max_completion_tokens: input.maxCompletionTokens,
        n: 1,
        stream: false,
        temperature: 0.72,
        response_format: {
          type: 'json_schema',
          json_schema: {
            name: 'lm_comment_options',
            description: 'Exactly the requested number of distinct comment options.',
            schema: providerJsonSchema(input.optionCount),
            strict: true,
          },
        },
      }, {
        maxRetries: 0,
        timeout: GROQ_REQUEST_TIMEOUT_MS,
        signal: input.signal,
      });

      const content = completion.choices[0]?.message.content;
      if (typeof content !== 'string' || content.length === 0) {
        throw new RelayError('INVALID_PROVIDER_RESPONSE');
      }
      return content;
    } catch (error) {
      throw mapGroqError(error, input.signal);
    }
  }
}

export function mapGroqError(error: unknown, signal?: AbortSignal): RelayError {
  if (error instanceof RelayError) return error;
  if (error instanceof RateLimitError) return new RelayError('PROVIDER_RATE_LIMIT');
  if (error instanceof APIConnectionTimeoutError) return new RelayError('PROVIDER_TIMEOUT');
  if (error instanceof APIUserAbortError && signal?.aborted) {
    return new RelayError('PROVIDER_TIMEOUT');
  }
  if (error instanceof APIConnectionError) return new RelayError('PROVIDER_UNAVAILABLE');
  if (error instanceof APIError) {
    if (error.status === 429) return new RelayError('PROVIDER_RATE_LIMIT');
    if (error.status === 408 || error.status === 504) return new RelayError('PROVIDER_TIMEOUT');
    return new RelayError('PROVIDER_UNAVAILABLE');
  }
  if (error instanceof DOMException && (error.name === 'AbortError' || error.name === 'TimeoutError')) {
    return new RelayError('PROVIDER_TIMEOUT');
  }

  return new RelayError('PROVIDER_UNAVAILABLE');
}
