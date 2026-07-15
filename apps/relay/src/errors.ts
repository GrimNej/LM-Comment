export const relayErrorCodes = [
  'BAD_REQUEST',
  'UNAUTHORIZED',
  'GENERATION_DISABLED',
  'RATE_LIMITED',
  'DAILY_LIMIT_REACHED',
  'PROVIDER_TIMEOUT',
  'PROVIDER_RATE_LIMIT',
  'PROVIDER_UNAVAILABLE',
  'INVALID_PROVIDER_RESPONSE',
  'INTERNAL',
] as const;

export type RelayErrorCode = typeof relayErrorCodes[number];

const errorDetails: Record<RelayErrorCode, { statusCode: number; message: string }> = {
  BAD_REQUEST: {
    statusCode: 400,
    message: 'The request is invalid. Review the text and generation settings.',
  },
  UNAUTHORIZED: {
    statusCode: 401,
    message: 'Demo access is not configured.',
  },
  GENERATION_DISABLED: {
    statusCode: 503,
    message: 'Generation is temporarily disabled.',
  },
  RATE_LIMITED: {
    statusCode: 429,
    message: 'The demo is receiving too many requests. Try again shortly.',
  },
  DAILY_LIMIT_REACHED: {
    statusCode: 429,
    message: 'The demo generation limit has been reached for today.',
  },
  PROVIDER_TIMEOUT: {
    statusCode: 504,
    message: 'Generation took too long. Check the connection and try again.',
  },
  PROVIDER_RATE_LIMIT: {
    statusCode: 429,
    message: 'The generation service is busy. Try again shortly.',
  },
  PROVIDER_UNAVAILABLE: {
    statusCode: 503,
    message: 'The generation service is unavailable. Try again shortly.',
  },
  INVALID_PROVIDER_RESPONSE: {
    statusCode: 502,
    message: 'The generation service returned an unusable response. Try again.',
  },
  INTERNAL: {
    statusCode: 500,
    message: 'The relay could not complete the request.',
  },
};

export class RelayError extends Error {
  readonly code: RelayErrorCode;
  readonly statusCode: number;

  constructor(code: RelayErrorCode) {
    const details = errorDetails[code];
    super(details.message);
    this.name = 'RelayError';
    this.code = code;
    this.statusCode = details.statusCode;
  }
}

export function errorBody(error: RelayError, requestId: string) {
  return {
    error: {
      code: error.code,
      message: error.message,
      requestId,
    },
  };
}

export function isBadRequestError(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false;
  const candidate = error as { statusCode?: unknown; validation?: unknown; code?: unknown };
  return candidate.statusCode === 400
    || candidate.statusCode === 404
    || candidate.statusCode === 413
    || Array.isArray(candidate.validation)
    || candidate.code === 'FST_ERR_CTP_BODY_TOO_LARGE';
}
